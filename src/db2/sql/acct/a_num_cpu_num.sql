-- PLAN CPU utilization by user and day (rebuild ASG-TMON report style)
SELECT
    SUBSYSTEM_ID                                             AS SUBSYSTEM_ID,     -- DB2 subsystem
    DATE(BEGIN_REC_TSTAMP)                                   AS DT,              -- summary date
    SUBSTR(CORRNAME, 1, 10)                                  AS USER_ID,         -- UP02CAID (first 10 chars)
    COUNT(*)                                                 AS DB_OCUR,         -- DB#OCUR: number of occurrences
    SUM(PREPARE)                                             AS DBPREP,          -- DBPREP: number of PREPAREs
    SUM(COMMIT)                                              AS DBACCOMM,        -- DBACCOMM: number of COMMITs
    SUM("INSERT" + "UPDATE" + "DELETE")                      AS DBDMLCNT,        -- DBDMLCNT: total DML statements
    SUM(GETPAGES)                                            AS DBBAGCET,        -- DBBAGCET: GETPAGEs  (adjust column name if needed)
    -- Class 1 (overall) elapsed and CPU
    SUM(CLASS1_ELAPSED)                                      AS DBC1TOEL,        -- class 1 elapsed time
    SUM(CLASS1_CPU_TOTAL)                                    AS DBC1TOCP,        -- class 1 CPU time
    SUM(CLASS1_IIP_CPU)                                      AS DBC1TOZP,        -- class 1 zIIP CPU time
    -- Class 2 (SQL) elapsed and CPU
    SUM(CLASS2_ELAPSED)                                      AS DBC2TOEL,        -- class 2 elapsed time
    SUM(CLASS2_CPU_TOTAL)                                    AS DBC2TOCP,        -- class 2 CPU time
    SUM(CLASS2_IIP_CPU)                                      AS DBC2TOZP,        -- class 2 zIIP CPU time
    -- Class 3 (suspensions etc.) elapsed; you can split further if needed
    SUM(CLASS3_ELAPSED)                                      AS DBC3TOEL,        -- class 3 elapsed time
    SUM(CLASS3_CPU_TOTAL)                                    AS DBC3TOEV,        -- class 3 CPU (if available)
    SUM(CLASS3_TCB_TIME)                                     AS DBC3TOIT,        -- class 3 TCB time (example)
    SUM(CLASS3_IIP_CPU)                                      AS DBC3TOIC         -- class 3 zIIP CPU (example)
FROM
    DTRBDR55.DB2PMFACCT_GENERAL                              -- adjust schema if needed
WHERE 1 = 1
    AND SUBSYSTEM_ID = 'PDBC'                                -- your DB2 subsystem
    -- restrict to 2025-10-01 ~ 2025-11-30
    AND DATE(BEGIN_REC_TSTAMP) BETWEEN DATE('2025-10-01') AND DATE('2025-11-30')
    -- match TMON: WHEN DBHCAID(3,8) = 'DSMEME00' / 'BDV20' THEN ACCEPT
    AND SUBSTR(CORRNAME, 3, 8) IN ('DSMEME00', 'BDV20')
GROUP BY
    SUBSYSTEM_ID,
    DATE(BEGIN_REC_TSTAMP),
    SUBSTR(CORRNAME, 1, 10)
ORDER BY
    SUBSYSTEM_ID,
    DT DESC,
    USER_ID;