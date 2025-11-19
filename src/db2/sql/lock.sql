-- =============================================
-- DB2 Lock Contention & Suspension Trend Report
-- Purpose : Analyze lock contention and IRLM suspension trends over time
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,          -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                       -- Number of records per day

    -- Lock contention metrics
    SUM(LOCK_REQ) AS TOTAL_LOCK_REQUESTS,           -- Total lock requests
    SUM(LOCK_ESC_EXCLUSIVE) AS EXCLUSIVE_ESCALATIONS, -- Number of exclusive lock escalations
    SUM(LOCK_ESC_SHARED) AS SHARED_ESCALATIONS,     -- Number of shared lock escalations

    -- Suspension and IRLM related metrics
    SUM(SUSPENSION_LOCK) AS LOCK_SUSPENSIONS,       -- Suspensions due to lock waits
    SUM(SUSPENSION_OTHER) AS OTHER_SUSPENSIONS,     -- Suspensions for non-lock reasons
    SUM(SUSP_IRLM_LATCH) AS IRLM_LATCH_SUSPENSIONS, -- IRLM latch suspensions
    SUM(IRLM_SRB_TIME) AS IRLM_SRB_CPU_TIME,        -- IRLM SRB CPU time
    SUM(IRLM_TCB_TIME) AS IRLM_TCB_CPU_TIME,        -- IRLM TCB CPU time

    -- Average values for trend analysis
    AVG(SUSPENSION_LOCK) AS AVG_LOCK_SUSP,          -- Average lock suspensions per record
    AVG(LOCK_ESC_EXCLUSIVE) AS AVG_EXCL_ESC,        -- Avg exclusive escalations per record
    AVG(LOCK_ESC_SHARED) AS AVG_SHARED_ESC          -- Avg shared escalations per record

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                         -- Specify target subsystem
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                       -- Define report time range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------


-- =============================================
-- DB2 Buffer Pool & I/O Efficiency Trend Report
-- Purpose : Analyze buffer pool usage, I/O load, and EDM pool efficiency
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,          -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                       -- Number of samples per day

    -- Buffer Pool performance
    SUM(PAGES_IN_STMT_POOL) AS STMT_POOL_PAGES,     -- Statement pool pages in use
    SUM(PAGES_IN_RDS_POOL) AS RDS_POOL_PAGES,       -- RDS pool pages in use
    SUM(PAGES_IN_SKEL_POOL) AS SKEL_POOL_PAGES,     -- Skeleton pool pages in use
    SUM(EDM_PAGES_DATSPACE) AS EDM_DATASPACE_PAGES, -- EDM data space usage
    SUM(EDM_DATASPACE_FULL) AS EDM_FULL_EVENTS,     -- EDM pool full events
    SUM(EDM_FREE_PGS_DATSP) AS EDM_FREE_PAGES,      -- Free pages in EDM pool

    -- I/O activity metrics
    SUM(LOG_READ_ACT_LOG) AS LOG_READ_ACTIVE,       -- Active log read operations
    SUM(LOG_READ_ARCH_LOG) AS LOG_READ_ARCHIVE,     -- Archive log read operations
    SUM(LOG_WRITE_IO_REQ) AS LOG_WRITE_IO_REQ,      -- Log write I/O requests
    SUM(LOG_WRITE_SUSPEND) AS LOG_WRITE_SUSPENDS,   -- Log write suspensions
    SUM(LOG_RECS_CREATED) AS LOG_RECS_CREATED,      -- Log records created
    SUM(LOG_CI_WRITTEN) AS LOG_CI_WRITTEN,          -- Log control intervals written

    -- Calculated efficiency ratios
    CASE WHEN SUM(PAGES_IN_STMT_POOL) > 0 THEN
         ROUND((SUM(PAGES_IN_STMT_POOL) - SUM(EDM_DATASPACE_FULL)) * 100.0 / SUM(PAGES_IN_STMT_POOL), 2)
    ELSE NULL END AS STMT_POOL_EFFICIENCY,          -- % Efficiency of statement pool

    CASE WHEN SUM(LOG_WRITE_IO_REQ) > 0 THEN
         ROUND(SUM(LOG_WRITE_SUSPEND) * 100.0 / SUM(LOG_WRITE_IO_REQ), 2)
    ELSE NULL END AS LOG_WRITE_SUSP_RATIO            -- % of suspended writes

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                         -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                       -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Commit / Rollback Throughput & Transaction Volume Trend Report
-- Purpose : Analyze commit/rollback throughput and transaction volume trend
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,             -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                          -- Number of samples per day

    -- Transaction activity metrics
    SUM(COMMIT) AS TOTAL_COMMITS,                      -- Total number of commits
    SUM(ROLLBACK) AS TOTAL_ROLLBACKS,                  -- Total number of rollbacks
    SUM(INSERT) AS TOTAL_INSERTS,                      -- Total number of inserted rows
    SUM(UPDATE) AS TOTAL_UPDATES,                      -- Total number of updated rows
    SUM(DELETE) AS TOTAL_DELETES,                      -- Total number of deleted rows
    SUM(FETCH) AS TOTAL_FETCHES,                       -- Total number of fetch operations

    -- Derived performance ratios
    CASE WHEN SUM(COMMIT + ROLLBACK) > 0 THEN
         ROUND(SUM(COMMIT) * 100.0 / (SUM(COMMIT) + SUM(ROLLBACK)), 2)
    ELSE NULL END AS COMMIT_RATIO,                     -- % of successful commits

    CASE WHEN SUM(COMMIT + ROLLBACK) > 0 THEN
         ROUND(SUM(ROLLBACK) * 100.0 / (SUM(COMMIT) + SUM(ROLLBACK)), 2)
    ELSE NULL END AS ROLLBACK_RATIO,                   -- % of rollbacks

    CASE WHEN COUNT(*) > 0 THEN
         ROUND((SUM(COMMIT) + SUM(ROLLBACK)) / COUNT(*), 2)
    ELSE NULL END AS AVG_TRANSACTIONS_PER_INTERVAL,    -- Avg transactions per record interval

    CASE WHEN SUM(FETCH) > 0 THEN
         ROUND((SUM(INSERT) + SUM(UPDATE) + SUM(DELETE)) * 100.0 / SUM(FETCH), 2)
    ELSE NULL END AS DML_TO_FETCH_RATIO                -- % ratio of DML vs fetch operations

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                            -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                          -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------

-- =============================================
-- DB2 Package / Statement Cache Efficiency Report
-- Purpose : Evaluate the efficiency of package reuse and statement caching
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,             -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                          -- Number of samples per day

    -- Package cache metrics
    SUM(PACKS_REBOUND) AS TOTAL_REBINDS,               -- Total number of package rebinds
    SUM(PACKS_FREED) AS TOTAL_FREED_PACKAGES,          -- Packages freed from EDM pool
    SUM(PACK_CACHOWRT_ENTRY) AS CACHE_WRITES,          -- Package cache write entries
    SUM(PACK_CACHOWRT_AUTID) AS CACHE_WRITES_AUTID,    -- Package cache writes (authid-specific)
    SUM(PKG_SEARCH_NF_LOC) AS SEARCH_NOT_FOUND_LOC,    -- Search not found (local)
    SUM(PKG_SEARCH_NF_DEL) AS SEARCH_NOT_FOUND_DEL,    -- Search not found (deleted)
    SUM(PKG_AUT_UNSUC_CACH) AS UNSUCCESSFUL_CACHES,    -- Unsuccessful cache attempts
    SUM(PKG_AUT_WO_CATALG) AS AUTONOMOUS_NO_CATALOG,   -- Packages without catalog ref

    -- Statement cache metrics
    SUM(STMT_CACHE_INSERTS) AS STMT_CACHE_INSERTS,     -- Statement cache inserts
    SUM(STMT_CACHE_REQUESTS) AS STMT_CACHE_REQUESTS,   -- Statement cache requests
    SUM(STMT_IN_GLOB_CACHE) AS STMT_IN_GLOBAL_CACHE,   -- Statements in global cache
    SUM(STMT_CACHE_REQUESTS) - SUM(STMT_CACHE_INSERTS) AS STMT_CACHE_MISS, -- Cache misses

    -- Derived cache efficiency ratios
    CASE WHEN SUM(STMT_CACHE_REQUESTS) > 0 THEN
         ROUND(SUM(STMT_CACHE_INSERTS) * 100.0 / SUM(STMT_CACHE_REQUESTS), 2)
    ELSE NULL END AS STMT_CACHE_HIT_RATIO,             -- % of successful statement cache hits

    CASE WHEN SUM(PACKS_REBOUND + PACKS_FREED) > 0 THEN
         ROUND(SUM(PACKS_REBOUND) * 100.0 / (SUM(PACKS_REBOUND) + SUM(PACKS_FREED)), 2)
    ELSE NULL END AS PACKAGE_REUSE_RATIO               -- % of packages reused successfully

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                            -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                          -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Parallelism & Workfile Usage Trend Report
-- Purpose : Monitor query parallelism level, sort activity, and workfile utilization trends
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,            -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                         -- Number of samples per day

    -- Parallelism metrics
    SUM(PAR_MAX_DEGREE) AS MAX_PARALLEL_DEGREE,       -- Maximum parallel degree observed
    SUM(PAR_MAX_DEGREE_EST) AS EST_PARALLEL_DEGREE,   -- Estimated max degree
    SUM(PAR_GROUPS_EXEC) AS PARALLEL_GROUPS_EXEC,     -- Parallel groups executed
    SUM(PAR_MEMBER_SKIPPED) AS MEMBER_SKIPPED,        -- Skipped members in parallel execution
    SUM(PAR_SYS_PLANNED) AS PARALLEL_SYS_PLANNED,     -- Parallel systems planned
    SUM(PAR_SEQ_AUTON_PROC) AS PAR_SEQ_AUTON_PROC,    -- Sequential autonomous processes

    -- Workfile and sort usage metrics
    SUM(WORKFILE_AGENT_USE) AS AGENT_WORKFILES_USED,  -- Workfiles used by agents
    SUM(WORKFILE_MAX_STOR) AS MAX_WORKFILE_STORAGE,   -- Max workfile storage used
    SUM(WORKFILE_CUR_STOR) AS CUR_WORKFILE_STORAGE,   -- Current workfile storage used
    SUM(WORKFILE_TOTL_CONF) AS TOTAL_WORKFILE_CONF,   -- Workfile configuration total
    SUM(WORKFILE_UNAV_4K) AS UNAVAILABLE_4K,          -- Unavailable 4K workfiles
    SUM(WORKFILE_STOR_4K) AS USED_4K_WORKFILES,       -- Used 4K workfiles
    SUM(WORKFILE_STOR_32K) AS USED_32K_WORKFILES,     -- Used 32K workfiles

    -- Sort activity metrics
    SUM(WORK_CUR_ACT_SORT) AS CUR_SORT_ACTIVE,        -- Current active sorts
    SUM(WORK_MAX_ACT_SORT) AS MAX_SORT_ACTIVE,        -- Max concurrent sorts
    SUM(WORK_NSORT_OVERFL) AS SORT_OVERFLOWS,         -- Sort overflow events
    SUM(SORT_FEEDBACK) AS SORT_FEEDBACKS,             -- Sort feedbacks issued

    -- Derived performance ratios
    CASE WHEN SUM(WORKFILE_CUR_STOR) > 0 THEN
         ROUND(SUM(WORK_NSORT_OVERFL) * 100.0 / SUM(WORKFILE_CUR_STOR), 2)
    ELSE NULL END AS SORT_OVERFLOW_RATIO,             -- % of sorts overflowed

    CASE WHEN SUM(PAR_MAX_DEGREE) > 0 THEN
         ROUND(SUM(PAR_GROUPS_EXEC) * 100.0 / SUM(PAR_MAX_DEGREE), 2)
    ELSE NULL END AS PARALLELISM_EFFICIENCY           -- % efficiency of parallel execution

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                           -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                         -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Logging Performance & Archive Efficiency Report
-- Purpose : Monitor DB2 logging throughput, archive activity, and suspension efficiency
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,            -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                         -- Number of samples per day

    -- Log write and read activity
    SUM(LOG_WRITE_IO_REQ) AS LOG_WRITE_IO_REQ,        -- Number of log write I/O requests
    SUM(LOG_WRITE_SUSPEND) AS LOG_WRITE_SUSPEND,      -- Number of suspended log writes
    SUM(LOG_WRITE_ASYN_REQ) AS LOG_WRITE_ASYNC_REQ,   -- Asynchronous log write requests
    SUM(LOG_CI_WRITTEN) AS LOG_CI_WRITTEN,            -- Log control intervals written
    SUM(LOG_RECS_CREATED) AS LOG_RECS_CREATED,        -- Total log records created
    SUM(LOG_RECS_PERFORMED) AS LOG_RECS_PERFORMED,    -- Log records actually written
    SUM(LOG_RECS_RETURNED) AS LOG_RECS_RETURNED,      -- Log records returned to requester

    -- Archive and read performance
    SUM(LOG_READ_ACT_LOG) AS LOG_READ_ACTIVE,         -- Reads from active log
    SUM(LOG_READ_ARCH_LOG) AS LOG_READ_ARCHIVE,       -- Reads from archive log
    SUM(ARCH_CI_CREATED) AS ARCHIVE_CI_CREATED,       -- Archive CI created
    SUM(ARCH_CI_OFFLOADED) AS ARCHIVE_CI_OFFLOADED,   -- Archive CI offloaded
    SUM(ARCH_READ_ALLOC) AS ARCHIVE_READ_ALLOC,       -- Archive read allocations

    -- Suspension and backlog indicators
    SUM(LOG_SUSP_ASY_DUP) AS ASYNC_DUPLICATE_SUSP,    -- Async duplicate suspensions
    SUM(LOG_SUSP_T_ASY_DUP) AS TOTAL_ASYNC_SUSP,      -- Total async suspensions
    SUM(LOG_WRITE_SUSPEND) - SUM(LOG_SUSP_ASY_DUP) AS TRUE_WRITE_BLOCKS,  -- True blocking suspends

    -- Derived performance ratios
    CASE WHEN SUM(LOG_WRITE_IO_REQ) > 0 THEN
         ROUND(SUM(LOG_WRITE_SUSPEND) * 100.0 / SUM(LOG_WRITE_IO_REQ), 2)
    ELSE NULL END AS LOG_SUSPEND_RATIO,               -- % of suspended writes

    CASE WHEN SUM(LOG_RECS_CREATED) > 0 THEN
         ROUND(SUM(LOG_RECS_PERFORMED) * 100.0 / SUM(LOG_RECS_CREATED), 2)
    ELSE NULL END AS LOG_WRITE_SUCCESS_RATIO,         -- % of successful log writes

    CASE WHEN SUM(LOG_READ_ACTIVE + LOG_READ_ARCHIVE) > 0 THEN
         ROUND(SUM(LOG_READ_ARCHIVE) * 100.0 / (SUM(LOG_READ_ACTIVE + LOG_READ_ARCHIVE)), 2)
    ELSE NULL END AS ARCHIVE_READ_RATIO               -- % of reads from archive logs

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                           -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                         -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Deadlock, Timeout & Contention Summary Report
-- Purpose : Analyze concurrency issues such as deadlocks, timeouts, and latch contention
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,               -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                            -- Number of samples per day

    -- Lock contention metrics
    SUM(LOCK_REQ) AS TOTAL_LOCK_REQUESTS,                -- Total lock requests
    SUM(LOCK_ESC_EXCLUSIVE) AS LOCK_ESCAL_EXCL,          -- Lock escalations (exclusive)
    SUM(LOCK_ESC_SHARED) AS LOCK_ESCAL_SHARED,           -- Lock escalations (shared)
    SUM(LOCK_TABLE) AS LOCK_TABLES,                      -- Table-level locks acquired

    -- Deadlock and timeout counts
    SUM(DEADLOCK) AS TOTAL_DEADLOCKS,                    -- Total deadlocks detected
    SUM(TIMEOUT) AS TOTAL_TIMEOUTS,                      -- Total timeouts detected
    SUM(DEADLOCK_EXITS) AS DEADLOCK_EXITS,               -- Threads exited due to deadlock
    SUM(TIMEOUT_EXITS) AS TIMEOUT_EXITS,                 -- Threads exited due to timeout

    -- IRLM and resource contention
    SUM(IRLM_PURGE_TIMEOUT) AS IRLM_PURGED_TIMEOUTS,     -- IRLM purged timeouts
    SUM(SUSPENSION_LOCK) AS LOCK_SUSPENDS,               -- Suspensions due to locks
    SUM(SUSPENSION_OTHER) AS OTHER_SUSPENDS,             -- Suspensions for other reasons
    SUM(RES_LTCH_CONT) AS RESOURCE_LATCH_CONTENTION,     -- Latch contention count
    SUM(SEC_CNT_M_LTCH_HLD) AS SECONDARY_LATCH_HOLD,     -- Secondary latch holds
    SUM(CUR_LK_TIMEOUT_APP) AS CURRENT_LOCK_TIMEOUTS,    -- Application-level timeouts

    -- Derived performance ratios
    CASE WHEN SUM(LOCK_REQ) > 0 THEN
         ROUND(SUM(DEADLOCK) * 100.0 / SUM(LOCK_REQ), 4)
    ELSE NULL END AS DEADLOCK_RATIO,                     -- % of deadlocks among lock requests

    CASE WHEN SUM(LOCK_REQ) > 0 THEN
         ROUND(SUM(TIMEOUT) * 100.0 / SUM(LOCK_REQ), 4)
    ELSE NULL END AS TIMEOUT_RATIO,                      -- % of timeouts among lock requests

    CASE WHEN SUM(SUSPENSION_LOCK + SUSPENSION_OTHER) > 0 THEN
         ROUND(SUM(SUSPENSION_LOCK) * 100.0 / (SUM(SUSPENSION_LOCK + SUSPENSION_OTHER)), 2)
    ELSE NULL END AS LOCK_SUSPEND_RATIO,                 -- % of suspends caused by locks

    CASE WHEN SUM(RES_LTCH_CONT + SEC_CNT_M_LTCH_HLD) > 0 THEN
         ROUND(SUM(RES_LTCH_CONT) * 100.0 /
               (SUM(RES_LTCH_CONT + SEC_CNT_M_LTCH_HLD)), 2)
    ELSE NULL END AS LATCH_CONTENTION_RATIO               -- % of latch contention vs total latch events

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                              -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                           -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Thread Activity & DBAT Performance Summary Report
-- Purpose : Analyze thread usage, DBAT efficiency, and idle timeout behavior
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,              -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                           -- Number of samples per day

    -- Active and inactive thread metrics
    SUM(ACTIVE_DBATS) AS ACTIVE_DBATS_TOTAL,            -- Active DBATs during the interval
    SUM(INACTIVE_DBATS) AS INACTIVE_DBATS_TOTAL,        -- Inactive DBATs
    SUM(INACT_DBATS_T2) AS INACT_DBATS_TYPE2,           -- Inactive DBATs (Type 2)
    SUM(DBATS_CREATED) AS DBATS_CREATED_TOTAL,          -- DBATs created during interval
    SUM(DBATS_NOT_USED) AS DBATS_NOT_USED,              -- DBATs not used after creation
    SUM(DBATS_BD_KEEPD) AS DBATS_BOUND_KEPT,            -- DBATs kept bound
    SUM(DBATS_BD_DEALL) AS DBATS_BOUND_DEALLOC,         -- DBATs deallocated
    SUM(DBATS_TERM_POOLINAC) AS DBATS_TERM_INACTIVE,    -- DBATs terminated due to inactivity
    SUM(DBATS_TERM_REUSE) AS DBATS_TERM_REUSED,         -- DBATs terminated and reused

    -- Queued / suspended metrics
    SUM(DBAT_QUEUED) AS DBATS_QUEUED,                   -- DBATs queued waiting for availability
    SUM(DBAT_SUS_PR_EX) AS DBATS_SUSP_PREEMPT,          -- DBATs suspended preemptively
    SUM(CLIENT_CONN_QUEUED) AS CLIENT_CONN_QUEUED,      -- Client connection queued
    SUM(CLIENT_CONN_WAITED) AS CLIENT_CONN_WAITED,      -- Client connection waited
    SUM(CRT_THREAD_QUEUED) AS CREATE_THREAD_QUEUED,     -- Threads queued for creation

    -- Thread termination / reclaim
    SUM(TERMINATE_THREAD) AS THREAD_TERMINATED,         -- Threads terminated
    SUM(CANCEL_THREAD) AS THREAD_CANCELLED,             -- Threads cancelled
    SUM(CONNECT_TERM_T1) AS CONNECT_TERM_TYPE1,         -- Type 1 connection terminations
    SUM(CONVERT_RW_TO_RO) AS THREAD_CONVERTED,          -- Threads converted RW → RO

    -- Derived performance indicators
    CASE WHEN SUM(ACTIVE_DBATS + INACTIVE_DBATS) > 0 THEN
         ROUND(SUM(ACTIVE_DBATS) * 100.0 /
               (SUM(ACTIVE_DBATS + INACTIVE_DBATS)), 2)
    ELSE NULL END AS ACTIVE_DBAT_RATIO,                 -- % of active DBATs

    CASE WHEN SUM(DBATS_CREATED) > 0 THEN
         ROUND(SUM(DBATS_TERM_INACTIVE) * 100.0 / SUM(DBATS_CREATED), 2)
    ELSE NULL END AS INACTIVE_DBAT_TERMINATION_RATIO,   -- % of DBATs terminated by inactivity

    CASE WHEN SUM(DBAT_QUEUED + CLIENT_CONN_QUEUED) > 0 THEN
         ROUND(SUM(CLIENT_CONN_WAITED) * 100.0 /
               (SUM(DBAT_QUEUED + CLIENT_CONN_QUEUED)), 2)
    ELSE NULL END AS CONNECTION_WAIT_RATIO              -- % of queued connections that waited

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                             -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                          -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 CPU Utilization & zIIP Offload Efficiency Report
-- Purpose : Monitor CPU utilization, zIIP offload rate, and processing efficiency
-- Data Source: DTRBDR55.DB2PM_STAT_GENERAL
-- =============================================

SELECT
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP) AS REPORT_DATE,             -- Aggregation date (daily)
    COUNT(*) AS RECORD_COUNT,                          -- Number of samples per day

    -- CPU utilization metrics
    SUM(CP_UTIL_DB2) AS DB2_CPU_UTILIZATION,           -- Total DB2 CPU utilization (%)
    SUM(CP_UTIL_DB2_DBM1) AS DBM1_CPU_UTILIZATION,     -- CPU utilization by DBM1 address space
    SUM(CP_UTIL_DB2_MSTR) AS MSTR_CPU_UTILIZATION,     -- CPU utilization by MSTR address space
    SUM(CP_UTIL_LPAR) AS LPAR_CPU_UTILIZATION,         -- Overall LPAR CPU utilization
    SUM(CPS_LPAR) AS LPAR_CPU_SHARES,                  -- LPAR CPU share usage (SMT-adjusted)

    -- CPU time distribution
    SUM(IRLM_TCB_TIME) AS IRLM_TCB_TIME,               -- IRLM TCB time consumption
    SUM(SSAS_TCB_TIME) AS SSAS_TCB_TIME,               -- SSAS TCB time
    SUM(DSAS_TCB_TIME) AS DSAS_TCB_TIME,               -- DSAS TCB time
    SUM(DDF_TCB_TIME) AS DDF_TCB_TIME,                 -- DDF TCB time
    SUM(IRLM_SRB_TIME) AS IRLM_SRB_TIME,               -- IRLM SRB time
    SUM(DDF_SRB_TIME) AS DDF_SRB_TIME,                 -- DDF SRB time
    SUM(DSAS_SRB_TIME) AS DSAS_SRB_TIME,               -- DSAS SRB time

    -- zIIP utilization metrics
    SUM(LR_ZIIP_ELIG_TOTAL) AS ZIIP_ELIGIBLE_TOTAL,    -- Total zIIP-eligible workload
    SUM(LR_ZIIP_TOTAL) AS ZIIP_USED_TOTAL,             -- Total zIIP CPU actually used
    SUM(LR_CPU_TOTAL) AS GENERAL_CPU_TOTAL,            -- Total general-purpose CPU used

    -- Derived performance ratios
    CASE WHEN SUM(LR_ZIIP_ELIG_TOTAL) > 0 THEN
         ROUND(SUM(LR_ZIIP_TOTAL) * 100.0 / SUM(LR_ZIIP_ELIG_TOTAL), 2)
    ELSE NULL END AS ZIIP_OFFLOAD_RATIO,               -- % of eligible workload offloaded to zIIP

    CASE WHEN SUM(LR_CPU_TOTAL) > 0 THEN
         ROUND(SUM(LR_ZIIP_TOTAL) * 100.0 / (SUM(LR_ZIIP_TOTAL + LR_CPU_TOTAL)), 2)
    ELSE NULL END AS TOTAL_OFFLOAD_RATIO,              -- % of total workload handled by zIIP

    CASE WHEN SUM(CP_UTIL_LPAR) > 0 THEN
         ROUND(SUM(CP_UTIL_DB2) * 100.0 / SUM(CP_UTIL_LPAR), 2)
    ELSE NULL END AS DB2_CPU_SHARE_RATIO               -- % of LPAR CPU used by DB2

FROM DTRBDR55.DB2PM_STAT_GENERAL
WHERE 1=1
  AND SUBSYSTEM_ID = 'PDBC'                            -- Specify subsystem ID
  AND BEGIN_REC_TSTAMP BETWEEN '2025-11-01-00.00.00' AND '2025-11-16-23.59.59'
                                                         -- Define report date range

GROUP BY SUBSYSTEM_ID, DATE(BEGIN_REC_TSTAMP)
ORDER BY REPORT_DATE DESC;
----------------------------------------------------------------------
-- =============================================
-- DB2 Top 10 Packages by CPU Usage Report
-- Purpose : Identify packages consuming the most CPU resources
-- Data Source: DB2PMFACCT_PROGRAM
-- =============================================

SELECT
    SUBSYSTEM_ID,
    PCK_COLLECTION_ID,
    PCK_ID,
    SUM(SQL_STMTS_ISSUED)  AS SQL_STMTS_ISSUED,  -- Total SQL statements issued
    SUM(CLASS7_CPU_TOTAL)  AS PKG_CPU_TIME,       -- CPU time of the package (class 7)
    SUM(CLASS7_ELAPSED)    AS PKG_ELAPSED_TIME    -- Elapsed time of the package (class 7)
FROM DB2PMFACCT_PROGRAM
WHERE 1 = 1
  AND SUBSYSTEM_ID = 'PDBC'                       -- Target DB2 subsystem
GROUP BY
    SUBSYSTEM_ID,
    PCK_COLLECTION_ID,
    PCK_ID
ORDER BY PKG_CPU_TIME DESC                        -- Order by package CPU usage
FETCH FIRST 10 ROWS ONLY;                         -- Top 10 packages
