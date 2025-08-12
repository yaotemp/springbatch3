package com.example.dataexport.generators;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * DB2 for z/OS – TINO allocator with:
 *  - Parallel streaming (source: table_b, targets: table_c null slots)
 *  - Window-level shuffle to avoid heavy ORDER BY RAND()
 *  - Bloom filter (persisted) for "already used" tracking (checkpointed)
 *  - Progress logging (console + CSV)
 *
 * Tables:
 *   table_a(seqno BIGINT, entity_type CHAR/VARCHAR)
 *   table_b(entity_type CHAR/VARCHAR, tino VARCHAR/CHAR)
 *   table_c(seqno BIGINT, tino VARCHAR/CHAR)
 * Assumption: table_a.seqno == table_c.seqno (1:1 mapping). entity_type ∈ {'B','C'}.
 */
public class TinoAllocatorZosHardcoded {

    /** ===== Hardcoded configuration (edit values below for your environment) ===== */
    static final class Config {
        // JDBC connection (edit to your DB2 for z/OS location)
        static final String JDBC_URL  = "jdbc:db2://127.0.0.1:50000/DSNLOCAT";
        static final String DB_USER   = "DB2USER";
        static final String DB_PASS   = "DB2PASS";

        // Batching
        static final int TARGET_BATCH = 20_000;  // number of target seqno to fetch per round
        static final int CAND_BATCH   = 40_000;  // number of candidate TINOs to buffer per round (>= TARGET_BATCH)

        // Bloom persistence
        static final String BLOOM_DIR = "/tmp/tino_bloom"; // USS dir on z/OS, ensure writable
        static final int CHECKPOINT_ROUNDS = 5;            // save bloom after every N committed rounds

        // Expected final assignments (controls bloom size & false positive rate)
        static final long EXPECTED_B = 120_000_000L;
        static final long EXPECTED_C = 120_000_000L;

        // Logging
        static final int  LOG_INTERVAL_SEC = 30; // periodic log snapshot
        static final String CSV_PATH = BLOOM_DIR + "/progress.csv";

        // Bloom parameters: ~1% FP at 12 bits/entry with k=7 (adjust if needed)
        static final int BLOOM_BITS_PER_ENTRY = 12;
        static final int BLOOM_K = 7;
    }

    public static void main(String[] args) {
        // Ensure bloom dir exists
        File bloomDir = new File(Config.BLOOM_DIR);
        if (!bloomDir.exists() && !bloomDir.mkdirs()) {
            System.err.println("Cannot create bloom dir: " + bloomDir.getAbsolutePath());
            System.exit(2);
        }

        // Load DB2 driver (optional on modern JVMs but safe)
        try { Class.forName("com.ibm.db2.jcc.DB2Driver"); } catch (Throwable ignore) {}

        log("Start. TARGET_BATCH=%d CAND_BATCH=%d checkpointEvery=%d rounds bloomDir=%s logInterval=%ds csv=%s",
                Config.TARGET_BATCH, Config.CAND_BATCH, Config.CHECKPOINT_ROUNDS,
                Config.BLOOM_DIR, Config.LOG_INTERVAL_SEC, Config.CSV_PATH);

        // Prepare CSV file (append mode, write header once)
        try { initCsv(new File(Config.CSV_PATH), CSV_HEADER); } catch (IOException e) {
            System.err.println("Initialize CSV failed: " + e.getMessage());
            System.exit(3);
        }

        try (Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USER, Config.DB_PASS)) {
            // Keep cursors over commit for streaming
            conn.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
            conn.setAutoCommit(false);

            processType(conn, "B", Config.EXPECTED_B);
            processType(conn, "C", Config.EXPECTED_C);

            log("All done.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Fatal JDBC error.");
            System.exit(4);
        }
    }

    /** Process one entity_type ('B' or 'C'). */
    private static void processType(Connection conn, String et, long expected) throws SQLException {
        log("== Type %s ==", et);

        File bloomFile = new File(Config.BLOOM_DIR, "bloom_" + et + ".bin");
        PersistBloom bloom;

        if (bloomFile.exists()) {
            try {
                bloom = PersistBloom.load(bloomFile);
                log("Loaded bloom for %s: bits=%d (%.1f MB) k=%d",
                        et, bloom.sizeBits(), bloom.memoryBytes()/1024.0/1024.0, bloom.k());
            } catch (IOException ex) {
                throw new SQLException("Failed to load bloom: " + bloomFile.getAbsolutePath(), ex);
            }
        } else {
            long bits = Math.max(64, roundUp64(expected * Config.BLOOM_BITS_PER_ENTRY));
            bloom = PersistBloom.create(bits, Config.BLOOM_K);
            log("Created new bloom for %s: expected=%d bits=%d (%.1f MB) k=%d",
                    et, expected, bloom.sizeBits(), bloom.memoryBytes()/1024.0/1024.0, bloom.k());
        }

        // Progress tracker
        Progress p = new Progress(et, Config.LOG_INTERVAL_SEC, new File(Config.CSV_PATH));

        // 1) Seed bloom from existing assignments in table_c (one linear pass)
        seedBloomFromUsed(conn, et, bloom, p);

        // 2) Open source stream from table_b (WITH UR to reduce locking)
        PreparedStatement psSrc = conn.prepareStatement(
                "SELECT b.tino FROM table_b b WHERE b.entity_type=? WITH UR",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        psSrc.setString(1, et);
        psSrc.setFetchSize(20_000);
        ResultSet rsSrc = psSrc.executeQuery();

        ArrayList<String> srcBuf = new ArrayList<>(Config.CAND_BATCH);

        long totalUpdated = 0L;
        int round = 0;

        while (true) {
            round++;

            // 3) Fetch a batch of target seqno (WITH UR; avoid ORDER BY for scalability)
            List<Long> targets = fetchTargets(conn, et, Config.TARGET_BATCH);
            if (targets.isEmpty()) {
                conn.commit();
                safeCheckpoint(bloom, bloomFile);
                p.logCommit(round, "assign", 0, srcBuf.size(), 0, totalUpdated);
                log("Type %s finished. Total updated: %,d", et, totalUpdated);
                break;
            }

            // 4) Fill source buffer forward-only; skip by bloom/window
            FillStats fs = fillSourceBuffer(rsSrc, bloom, srcBuf, Math.max(Config.CAND_BATCH, targets.size()));
            p.addSkipped(fs.skippedBloom, fs.skippedWindow);

            if (srcBuf.isEmpty()) {
                conn.commit();
                safeCheckpoint(bloom, bloomFile);
                p.logHeartbeat(round, "source_exhausted");
                log("Type %s: source exhausted or skipped by bloom. Total updated: %,d", et, totalUpdated);
                break;
            }

            // 5) Window-level shuffle to avoid positional bias
            Collections.shuffle(srcBuf);
            Collections.shuffle(targets);

            // 6) Assign 1:1 and update (guarded by tino IS NULL)
            int assign = Math.min(targets.size(), srcBuf.size());
            long updated = batchAssign(conn, targets, srcBuf, assign, bloom);
            conn.commit();

            totalUpdated += updated;
            p.addUpdated(updated);
            p.logCommit(round, "assign", targets.size(), srcBuf.size(), updated, totalUpdated);

            // 7) Periodic checkpoint of bloom after successful commit
            if (round % Config.CHECKPOINT_ROUNDS == 0) {
                safeCheckpoint(bloom, bloomFile);
                p.logHeartbeat(round, "checkpoint");
            }

            if (updated == 0) {
                safeCheckpoint(bloom, bloomFile);
                p.logHeartbeat(round, "no_progress");
                log("Type %s: no progress (maybe bloom FP or not enough unique candidates). Stop.", et);
                break;
            }

            // 8) Drop used candidates from buffer; keep remainder for next round
            if (assign >= srcBuf.size()) srcBuf.clear();
            else srcBuf.subList(0, assign).clear();

            // 9) Periodic progress snapshot
            p.maybeLogPeriodic(round, "running");
        }

        try { rsSrc.close(); } catch (Exception ignore) {}
        try { psSrc.close(); } catch (Exception ignore) {}

        log("== Type %s done. Updated: %,d ==", et, totalUpdated);
    }

    /** Seed bloom from existing table_c assignments for the given type. */
    private static void seedBloomFromUsed(Connection conn, String et, PersistBloom bloom, Progress p) throws SQLException {
        log("Seeding bloom from table_c used TINOs for type %s ...", et);
        long cnt = 0;
        long startNs = System.nanoTime();
        long lastNs  = startNs;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.tino FROM table_c c JOIN table_a a ON a.seqno=c.seqno " +
                        "WHERE a.entity_type=? AND c.tino IS NOT NULL WITH UR",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setString(1, et);
            ps.setFetchSize(20_000);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tino = rs.getString(1);
                    if (tino != null) {
                        bloom.put(tino);
                        cnt++;
                    }
                    long now = System.nanoTime();
                    if ((now - lastNs) >= p.logIntervalNanos) {
                        double elapsed = (now - startNs) / 1e9;
                        double rpsAvg  = cnt / Math.max(elapsed, 1e-6);
                        p.logCsv("seed", 0, 0, 0, 0, cnt, rpsAvg, rpsAvg);
                        log("Seed %s: loaded %,d (avg %.0f/s)", et, cnt, rpsAvg);
                        lastNs = now;
                    }
                }
            }
        }
        conn.commit(); // read-only but keep consistency
        double elapsed = (System.nanoTime() - startNs) / 1e9;
        log("Bloom seeded for %s: %,d items in %.1fs (avg %.0f/s).", et, cnt, elapsed, cnt / Math.max(elapsed, 1e-6));
    }

    /** Fetch up to 'limit' target seqno with null TINO for the given type. */
    private static List<Long> fetchTargets(Connection conn, String et, int limit) throws SQLException {
        String sql =
                "SELECT c.seqno " +
                "FROM table_c c JOIN table_a a ON a.seqno=c.seqno " +
                "WHERE a.entity_type=? AND c.tino IS NULL " +
                "FETCH FIRST " + limit + " ROWS ONLY WITH UR";
        ArrayList<Long> list = new ArrayList<>(limit);
        try (PreparedStatement ps = conn.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setString(1, et);
            ps.setFetchSize(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getLong(1));
            }
        }
        return list;
    }

    /** Fill the source candidate buffer forward-only from table_b stream, skipping by bloom and window dedup. */
    private static FillStats fillSourceBuffer(ResultSet rsSrc, PersistBloom bloom, List<String> buf, int need) throws SQLException {
        HashSet<String> windowSeen = new HashSet<>(Math.max(need, 16));
        for (String s : buf) windowSeen.add(s);
        long skippedBloom = 0;
        long skippedWindow = 0;

        while (buf.size() < need && rsSrc.next()) {
            String tino = rsSrc.getString(1);
            if (tino == null) continue;
            if (windowSeen.contains(tino)) { skippedWindow++; continue; }
            if (bloom.mightContain(tino))   { skippedBloom++;  continue; }
            buf.add(tino);
            windowSeen.add(tino);
        }
        return new FillStats(skippedBloom, skippedWindow);
    }

    /** Batch assign candidates to targets (1:1) and update table_c guarded by 'tino IS NULL'. */
    private static long batchAssign(Connection conn, List<Long> targets, List<String> srcBuf, int assign, PersistBloom bloom) throws SQLException {
        if (assign <= 0) return 0L;
        long updated = 0L;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE table_c SET tino=? WHERE seqno=? AND tino IS NULL")) {
            for (int i = 0; i < assign; i++) {
                String tino = srcBuf.get(i);
                Long   seq  = targets.get(i);
                // Insert into bloom immediately (in-memory). Persistence happens after commit by checkpoint.
                bloom.put(tino);
                ps.setString(1, tino);
                ps.setLong(2, seq);
                ps.addBatch();
            }
            int[] res = ps.executeBatch();
            for (int r : res) {
                if (r >= 0) updated += r;
                else if (r == Statement.SUCCESS_NO_INFO) updated += 1;
            }
        }
        return updated;
    }

    /** Atomic-ish checkpoint of bloom to file (write temp then rename). */
    private static void safeCheckpoint(PersistBloom bloom, File file) {
        try {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            bloom.save(tmp);
            if (file.exists() && !file.delete()) {
                throw new IOException("Cannot delete old bloom file: " + file.getAbsolutePath());
            }
            if (!tmp.renameTo(file)) {
                throw new IOException("Cannot move temp file to: " + file.getAbsolutePath());
            }
            System.out.printf("Bloom checkpoint saved: %s (%.1f MB)%n",
                    file.getAbsolutePath(), bloom.memoryBytes()/1024.0/1024.0);
        } catch (Exception e) {
            System.err.println("Checkpoint failed for " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /** CSV helpers */
    private static final String[] CSV_HEADER = new String[]{
            "timestamp","type","phase","round","targets","src_buf",
            "updated","total_updated","avg_rps","inst_rps",
            "skipped_bloom_total","skipped_window_total","heap_used_mb","heap_total_mb"
    };

    private static void initCsv(File csv, String[] header) throws IOException {
        boolean exists = csv.exists();
        try (FileOutputStream fos = new FileOutputStream(csv, true);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            if (!exists) {
                bw.write(String.join(",", header));
                bw.newLine();
            }
        }
    }

    private static void appendCsv(File csv, String[] cols) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(csv, true);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(String.join(",", escapeCsv(cols)));
            bw.newLine();
        }
    }

    private static String[] escapeCsv(String[] cols) {
        String[] out = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            String s = cols[i] == null ? "" : cols[i];
            boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
            if (needQuote) s = "\"" + s.replace("\"", "\"\"") + "\"";
            out[i] = s;
        }
        return out;
    }

    /** Simple logger */
    private static void log(String fmt, Object... args) {
        System.out.println(String.format(fmt, args));
    }

    /** Periodic progress tracker (console + CSV). */
    private static final class Progress {
        private final String type;
        private final long startNs;
        private long lastLogNs;
        private long totalUpdated;
        private long skippedBloomTotal;
        private long skippedWindowTotal;
        private final long logIntervalNanos;
        private final File csv;
        private long lastUpdatedAtLog;

        private static final DateTimeFormatter TS_FMT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        Progress(String type, int logIntervalSec, File csv) {
            this.type = type;
            this.startNs = System.nanoTime();
            this.lastLogNs = startNs;
            this.logIntervalNanos = Math.max(1, logIntervalSec) * 1_000_000_000L;
            this.csv = csv;
            this.lastUpdatedAtLog = 0L;
        }

        final long logIntervalNanos() { return logIntervalNanos; }

        void addUpdated(long n) { totalUpdated += n; }
        void addSkipped(long bloom, long window) { skippedBloomTotal += bloom; skippedWindowTotal += window; }

        void logCommit(int round, String phase, int targets, int srcBuf, long updated, long total) {
            long nowNs = System.nanoTime();
            double elapsedTot = (nowNs - startNs)/1e9;
            double elapsedStep = (nowNs - lastLogNs)/1e9;
            long deltaUpdated = total - lastUpdatedAtLog;

            double avgRps  = total / Math.max(elapsedTot, 1e-6);
            double instRps = deltaUpdated / Math.max(elapsedStep, 1e-6);

            Runtime rt = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024*1024);
            long totalMb = rt.totalMemory() / (1024*1024);

            String ts = TS_FMT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
            System.out.println(String.format(
                    "[%s] %s round=%d targets=%d src=%d updated=%d total=%d avg=%.0f/s inst=%.0f/s skipped(bloom=%d,window=%d) mem=%d/%d MB",
                    ts, type, round, targets, srcBuf, updated, total, avgRps, instRps, skippedBloomTotal, skippedWindowTotal, usedMb, totalMb));

            try {
                appendCsv(csv, new String[]{
                        ts, type, phase, String.valueOf(round),
                        String.valueOf(targets), String.valueOf(srcBuf),
                        String.valueOf(updated), String.valueOf(total),
                        String.format("%.3f", avgRps), String.format("%.3f", instRps),
                        String.valueOf(skippedBloomTotal), String.valueOf(skippedWindowTotal),
                        String.valueOf(usedMb), String.valueOf(totalMb)
                });
            } catch (IOException e) {
                System.err.println("Write CSV failed: " + e.getMessage());
            }

            lastLogNs = nowNs;
            lastUpdatedAtLog = total;
        }

        void logHeartbeat(int round, String phase) {
            String ts = TS_FMT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
            Runtime rt = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024*1024);
            long totalMb = rt.totalMemory() / (1024*1024);
            double elapsedTot = (System.nanoTime() - startNs)/1e9;
            double avgRps  = totalUpdated / Math.max(elapsedTot, 1e-6);
            try {
                appendCsv(csv, new String[]{
                        ts, type, phase, String.valueOf(round),
                        "0","0","0", String.valueOf(totalUpdated),
                        String.format("%.3f", avgRps), "0",
                        String.valueOf(skippedBloomTotal), String.valueOf(skippedWindowTotal),
                        String.valueOf(usedMb), String.valueOf(totalMb)
                });
            } catch (IOException e) {
                System.err.println("Write CSV failed: " + e.getMessage());
            }
        }

        void logCsv(String phase, int round, int targets, int srcBuf, long updated, long total, double avgRps, double instRps) {
            String ts = TS_FMT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
            Runtime rt = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024*1024);
            long totalMb = rt.totalMemory() / (1024*1024);
            try {
                appendCsv(csv, new String[]{
                        ts, type, phase, String.valueOf(round),
                        String.valueOf(targets), String.valueOf(srcBuf),
                        String.valueOf(updated), String.valueOf(total),
                        String.format("%.3f", avgRps), String.format("%.3f", instRps),
                        String.valueOf(skippedBloomTotal), String.valueOf(skippedWindowTotal),
                        String.valueOf(usedMb), String.valueOf(totalMb)
                });
            } catch (IOException e) {
                System.err.println("Write CSV failed: " + e.getMessage());
            }
        }

        void maybeLogPeriodic(int round, String phase) {
            long nowNs = System.nanoTime();
            if (nowNs - lastLogNs >= logIntervalNanos) {
                // Periodic snapshot (0 updated for this message)
                logCommit(round, phase, 0, 0, 0, totalUpdated);
            }
        }
    }

    /** Stats collected while filling the source buffer. */
    private static final class FillStats {
        final long skippedBloom;
        final long skippedWindow;
        FillStats(long b, long w) { this.skippedBloom = b; this.skippedWindow = w; }
    }

    /** Round up to multiple of 64 bits. */
    private static long roundUp64(long bits) { return ((bits + 63) / 64) * 64; }

    /** ========================= Persisted Bloom Filter ========================= */
    static final class PersistBloom {
        private final long[] bits;
        private final long sizeBits;
        private final long mask; // if power-of-two, use & mask; else use modulo
        private final int k;

        static PersistBloom create(long sizeBits, int k) { return new PersistBloom(sizeBits, k); }

        private PersistBloom(long sizeBits, int k) {
            long adj = roundUp64(Math.max(sizeBits, 64));
            this.sizeBits = adj;
            this.bits = new long[(int)(adj / 64)];
            this.k = k;
            this.mask = (isPowerOfTwo(adj) ? (adj - 1) : -1L);
        }

        long sizeBits() { return sizeBits; }
        long memoryBytes() { return bits.length * 8L; }
        int  k() { return k; }

        void put(String s) {
            Hash h = hash64x2(s);
            for (int i = 0; i < k; i++) {
                long idx = index(h.h1 + i * h.h2);
                setBit(idx);
            }
        }

        boolean mightContain(String s) {
            Hash h = hash64x2(s);
            for (int i = 0; i < k; i++) {
                long idx = index(h.h1 + i * h.h2);
                if (!getBit(idx)) return false;
            }
            return true;
        }

        void save(File f) throws IOException {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
                out.writeInt(0xB10F600D);   // magic
                out.writeInt(1);            // version
                out.writeLong(sizeBits);
                out.writeInt(k);
                out.writeInt(bits.length);
                for (long w : bits) out.writeLong(w);
            }
        }

        static PersistBloom load(File f) throws IOException {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                int magic = in.readInt();
                int ver = in.readInt();
                if (magic != 0xB10F600D || ver != 1) {
                    throw new IOException("Invalid bloom file format");
                }
                long sizeBits = in.readLong();
                int k = in.readInt();
                int len = in.readInt();
                PersistBloom b = new PersistBloom(sizeBits, k);
                if (b.bits.length != len) {
                    throw new IOException("Bloom size mismatch: file=" + len + " words, runtime=" + b.bits.length);
                }
                for (int i = 0; i < len; i++) b.bits[i] = in.readLong();
                return b;
            }
        }

        private long index(long h) {
            long x = mix64(h);
            if (mask != -1L) return x & mask;
            long v = x % sizeBits;
            return v < 0 ? v + sizeBits : v;
        }

        private void setBit(long bitIndex) {
            int word = (int)(bitIndex >>> 6);
            int off  = (int)(bitIndex & 63);
            bits[word] |= (1L << off);
        }

        private boolean getBit(long bitIndex) {
            int word = (int)(bitIndex >>> 6);
            int off  = (int)(bitIndex & 63);
            return (bits[word] & (1L << off)) != 0;
        }

        private static boolean isPowerOfTwo(long x) { return x > 0 && (x & (x - 1)) == 0; }

        // SplitMix64 mixer
        private static long mix64(long z) {
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }

        // Two 64-bit hashes derived from UTF-8 bytes (FNV-1a + mixed)
        private static Hash hash64x2(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            long h1 = fnv1a64(b);
            long h2 = mix64(h1 ^ 0x9E3779B97F4A7C15L ^ (b.length * 0x9E3779B97F4A7C15L));
            if (h2 == 0) h2 = 0xBF58476D1CE4E5B9L;
            return new Hash(h1, h2);
        }

        private static long fnv1a64(byte[] data) {
            long hash = 0xcbf29ce484222325L;
            for (byte datum : data) {
                hash ^= (datum & 0xff);
                hash *= 0x100000001b3L;
            }
            return hash;
        }

        private static final class Hash {
            final long h1, h2;
            Hash(long h1, long h2) { this.h1 = h1; this.h2 = h2; }
        }
    }
}
