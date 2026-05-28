WITH summary_2023 AS (
    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK,
        SUM("COUNT") AS count_2023,
        SUM(CLASS1_ELAPSED) AS elapsed_2023,
        SUM(CLASS1_CPU_TOTAL) AS cpu_2023
    FROM DTRBD023.SUM_GENERAL
    WHERE
        PLAN_NAME LIKE 'db2jcc_a%'
        OR PLAN_NAME LIKE 'PTR%'
        OR PLAN_NAME = 'DSNTEP2'
    GROUP BY
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
),
summary_2024 AS (
    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK,
        SUM("COUNT") AS count_2024,
        SUM(CLASS1_ELAPSED) AS elapsed_2024,
        SUM(CLASS1_CPU_TOTAL) AS cpu_2024
    FROM DTRBD024.SUM_GENERAL
    WHERE
        PLAN_NAME LIKE 'db2jcc_a%'
        OR PLAN_NAME LIKE 'PTR%'
        OR PLAN_NAME = 'DSNTEP2'
    GROUP BY
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
),
summary_2025 AS (
    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK,
        SUM("COUNT") AS count_2025,
        SUM(CLASS1_ELAPSED) AS elapsed_2025,
        SUM(CLASS1_CPU_TOTAL) AS cpu_2025
    FROM DTRBD025.SUM_GENERAL
    WHERE
        PLAN_NAME LIKE 'db2jcc_a%'
        OR PLAN_NAME LIKE 'PTR%'
        OR PLAN_NAME = 'DSNTEP2'
    GROUP BY
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
),
all_keys AS (
    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
    FROM summary_2023

    UNION

    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
    FROM summary_2024

    UNION

    SELECT
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK
    FROM summary_2025
),
base_result AS (
    SELECT
        k."MONTH",
        k.CONNECT_TYPE,
        k.CONNECT_ID,
        k.CORRNAME,
        k.PLAN_NAME,
        k.PRIMAUTH,
        k.MAINPACK,

        COALESCE(s23.count_2023, 0) AS count_2023,
        COALESCE(s24.count_2024, 0) AS count_2024,
        COALESCE(s25.count_2025, 0) AS count_2025,

        COALESCE(s23.elapsed_2023, 0) AS elapsed_2023,
        COALESCE(s24.elapsed_2024, 0) AS elapsed_2024,
        COALESCE(s25.elapsed_2025, 0) AS elapsed_2025,

        COALESCE(s23.cpu_2023, 0) AS cpu_2023,
        COALESCE(s24.cpu_2024, 0) AS cpu_2024,
        COALESCE(s25.cpu_2025, 0) AS cpu_2025

    FROM all_keys k
    LEFT JOIN summary_2023 s23
        ON k."MONTH" = s23."MONTH"
       AND k.CONNECT_TYPE = s23.CONNECT_TYPE
       AND k.CONNECT_ID = s23.CONNECT_ID
       AND k.CORRNAME = s23.CORRNAME
       AND k.PLAN_NAME = s23.PLAN_NAME
       AND k.PRIMAUTH = s23.PRIMAUTH
       AND k.MAINPACK = s23.MAINPACK
    LEFT JOIN summary_2024 s24
        ON k."MONTH" = s24."MONTH"
       AND k.CONNECT_TYPE = s24.CONNECT_TYPE
       AND k.CONNECT_ID = s24.CONNECT_ID
       AND k.CORRNAME = s24.CORRNAME
       AND k.PLAN_NAME = s24.PLAN_NAME
       AND k.PRIMAUTH = s24.PRIMAUTH
       AND k.MAINPACK = s24.MAINPACK
    LEFT JOIN summary_2025 s25
        ON k."MONTH" = s25."MONTH"
       AND k.CONNECT_TYPE = s25.CONNECT_TYPE
       AND k.CONNECT_ID = s25.CONNECT_ID
       AND k.CORRNAME = s25.CORRNAME
       AND k.PLAN_NAME = s25.PLAN_NAME
       AND k.PRIMAUTH = s25.PRIMAUTH
       AND k.MAINPACK = s25.MAINPACK
)
SELECT
    "MONTH",
    CONNECT_TYPE,
    CONNECT_ID,
    CORRNAME,
    PLAN_NAME,
    PRIMAUTH,
    MAINPACK,

    count_2023,
    count_2024,
    count_2025,

    count_2024 - count_2023 AS count_2024_vs_2023_change,
    CASE
        WHEN count_2023 = 0 THEN NULL
        ELSE DECIMAL((count_2024 - count_2023) * 100.0 / count_2023, 18, 2)
    END AS count_2024_vs_2023_pct,

    count_2025 - count_2024 AS count_2025_vs_2024_change,
    CASE
        WHEN count_2024 = 0 THEN NULL
        ELSE DECIMAL((count_2025 - count_2024) * 100.0 / count_2024, 18, 2)
    END AS count_2025_vs_2024_pct,

    elapsed_2023,
    elapsed_2024,
    elapsed_2025,

    elapsed_2024 - elapsed_2023 AS elapsed_2024_vs_2023_change,
    CASE
        WHEN elapsed_2023 = 0 THEN NULL
        ELSE DECIMAL((elapsed_2024 - elapsed_2023) * 100.0 / elapsed_2023, 18, 2)
    END AS elapsed_2024_vs_2023_pct,

    elapsed_2025 - elapsed_2024 AS elapsed_2025_vs_2024_change,
    CASE
        WHEN elapsed_2024 = 0 THEN NULL
        ELSE DECIMAL((elapsed_2025 - elapsed_2024) * 100.0 / elapsed_2024, 18, 2)
    END AS elapsed_2025_vs_2024_pct,

    cpu_2023,
    cpu_2024,
    cpu_2025,

    cpu_2024 - cpu_2023 AS cpu_2024_vs_2023_change,
    CASE
        WHEN cpu_2023 = 0 THEN NULL
        ELSE DECIMAL((cpu_2024 - cpu_2023) * 100.0 / cpu_2023, 18, 2)
    END AS cpu_2024_vs_2023_pct,

    cpu_2025 - cpu_2024 AS cpu_2025_vs_2024_change,
    CASE
        WHEN cpu_2024 = 0 THEN NULL
        ELSE DECIMAL((cpu_2025 - cpu_2024) * 100.0 / cpu_2024, 18, 2)
    END AS cpu_2025_vs_2024_pct

FROM base_result
ORDER BY
    CONNECT_TYPE,
    CONNECT_ID,
    CORRNAME,
    PLAN_NAME,
    PRIMAUTH,
    MAINPACK,
    "MONTH";