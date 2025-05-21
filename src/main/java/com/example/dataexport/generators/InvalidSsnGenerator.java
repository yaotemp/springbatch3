package com.example.dataexport.generators;

import com.example.dataexport.util.Provider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class InvalidSsnGenerator {

    private static final String OUTPUT_FILE_NAME = "all_invalid_ssns.txt";
    private static final long TARGET_COUNT = 250_000_000L;
    private static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        System.out.println("Starting generation of 250 million invalid SSNs (all-digit and alpha-numeric). Output file: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());

        long invalidSsnCounter = 0;
        long checkedSsnCounter = 0;
        long startTime = System.currentTimeMillis();
        Set<String> seen = new HashSet<>(1_000_000); // Only used for alpha-numeric to avoid duplicates

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_NAME))) {
            // 1. Generate all all-digit invalid SSNs
            outer:
            for (int area = 0; area <= 999; area++) {
                for (int group = 0; group <= 99; group++) {
                    for (int serial = 0; serial <= 9999; serial++) {
                        checkedSsnCounter++;
                        if (Provider.isSsnInvalidAccordingToMd(area, group, serial)) {
                            String ssn = String.format("%03d-%02d-%04d", area, group, serial);
                            writer.write(ssn + "\n");
                            invalidSsnCounter++;
                            if (invalidSsnCounter % 1_000_000 == 0) {
                                logProgress("All-digit", invalidSsnCounter, startTime);
                            }
                            if (invalidSsnCounter >= TARGET_COUNT) {
                                break outer;
                            }
                        }
                    }
                }
            }
            // 2. If not enough, generate random alpha-numeric invalid SSNs
            while (invalidSsnCounter < TARGET_COUNT) {
                char first = UPPERCASE[RANDOM.nextInt(UPPERCASE.length)];
                int d2 = RANDOM.nextInt(10);
                int d3 = RANDOM.nextInt(10);
                int g1 = RANDOM.nextInt(10);
                int g2 = RANDOM.nextInt(10);
                int s1 = RANDOM.nextInt(10);
                int s2 = RANDOM.nextInt(10);
                int s3 = RANDOM.nextInt(10);
                int s4 = RANDOM.nextInt(10);
                String ssn = String.format("%c%1d%1d-%1d%1d-%1d%1d%1d%1d", first, d2, d3, g1, g2, s1, s2, s3, s4);
                // Avoid duplicates with all-digit set (not strictly needed, but avoid within alpha-numeric)
                if (seen.add(ssn)) {
                    writer.write(ssn + "\n");
                    invalidSsnCounter++;
                    if (invalidSsnCounter % 1_000_000 == 0) {
                        logProgress("Alpha-numeric", invalidSsnCounter, startTime);
                    }
                }
            }
            writer.flush();
            logCompletion(invalidSsnCounter, startTime);
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Output file located at: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());
    }

    private static void logProgress(String stage, long count, long startTime) {
        long now = System.currentTimeMillis();
        double elapsed = (now - startTime) / 1000.0;
        System.out.printf("[%s] Generated: %d, Elapsed: %.2f sec\n", stage, count, elapsed);
    }

    private static void logCompletion(long count, long startTime) {
        long now = System.currentTimeMillis();
        double elapsed = (now - startTime) / 1000.0;
        System.out.printf("--- Generation Complete ---\nTotal generated: %d\nTotal time: %.2f sec (%.2f min)\n", count, elapsed, elapsed / 60.0);
    }
} 