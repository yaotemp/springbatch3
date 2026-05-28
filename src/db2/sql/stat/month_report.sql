WITH summary_2025 AS (
    SELECT
        2025 AS YEAR,
        "MONTH",
        CONNECT_TYPE,
        CONNECT_ID,
        CORRNAME,
        PLAN_NAME,
        PRIMAUTH,
        MAINPACK,
        SUM(COUNT) AS sum_count,
        SUM(CLASS1_ELAPSED) AS sum_class1_elapsed,
        SUM(CLASS1_CPU_TOTAL) AS sum_class1_cpu_total
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
summary_with_prev_month AS (
    SELECT
        curr.YEAR,
        curr."MONTH",
        curr.CONNECT_TYPE,
        curr.CONNECT_ID,
        curr.CORRNAME,
        curr.PLAN_NAME,
        curr.PRIMAUTH,
        curr.MAINPACK,
        curr.sum_count,
        curr.sum_class1_elapsed,
        curr.sum_class1_cpu_total,
        MAX(prev_month."MONTH") AS prev_month
    FROM summary_2025 curr
    LEFT JOIN summary_2025 prev_month
        ON curr.CONNECT_TYPE = prev_month.CONNECT_TYPE
       AND curr.CONNECT_ID = prev_month.CONNECT_ID
       AND curr.CORRNAME = prev_month.CORRNAME
       AND curr.PLAN_NAME = prev_month.PLAN_NAME
       AND curr.PRIMAUTH = prev_month.PRIMAUTH
       AND curr.MAINPACK = prev_month.MAINPACK
       AND prev_month."MONTH" < curr."MONTH"
    GROUP BY
        curr.YEAR,
        curr."MONTH",
        curr.CONNECT_TYPE,
        curr.CONNECT_ID,
        curr.CORRNAME,
        curr.PLAN_NAME,
        curr.PRIMAUTH,
        curr.MAINPACK,
        curr.sum_count,
        curr.sum_class1_elapsed,
        curr.sum_class1_cpu_total
)
SELECT
    curr.YEAR,
    curr."MONTH",
    curr.CONNECT_TYPE,
    curr.CONNECT_ID,
    curr.CORRNAME,
    curr.PLAN_NAME,
    curr.PRIMAUTH,
    curr.MAINPACK,

    curr.sum_count,
    prev.sum_count AS prev_sum_count,
    curr.sum_count - COALESCE(prev.sum_count, 0) AS count_change,

    CASE
        WHEN prev.sum_count IS NULL OR prev.sum_count = 0 THEN NULL
        ELSE DECIMAL(
            (curr.sum_count - prev.sum_count) * 100.0 / prev.sum_count,
            18,
            2
        )
    END AS count_change_pct,

    curr.sum_class1_elapsed,
    prev.sum_class1_elapsed AS prev_sum_class1_elapsed,
    curr.sum_class1_elapsed - COALESCE(prev.sum_class1_elapsed, 0) AS elapsed_change,

    CASE
        WHEN prev.sum_class1_elapsed IS NULL OR prev.sum_class1_elapsed = 0 THEN NULL
        ELSE DECIMAL(
            (curr.sum_class1_elapsed - prev.sum_class1_elapsed) * 100.0 / prev.sum_class1_elapsed,
            18,
            2
        )
    END AS elapsed_change_pct,

    curr.sum_class1_cpu_total,
    prev.sum_class1_cpu_total AS prev_sum_class1_cpu_total,
    curr.sum_class1_cpu_total - COALESCE(prev.sum_class1_cpu_total, 0) AS cpu_change,

    CASE
        WHEN prev.sum_class1_cpu_total IS NULL OR prev.sum_class1_cpu_total = 0 THEN NULL
        ELSE DECIMAL(
            (curr.sum_class1_cpu_total - prev.sum_class1_cpu_total) * 100.0 / prev.sum_class1_cpu_total,
            18,
            2
        )
    END AS cpu_change_pct

FROM summary_with_prev_month curr
LEFT JOIN summary_2025 prev
    ON curr.CONNECT_TYPE = prev.CONNECT_TYPE
   AND curr.CONNECT_ID = prev.CONNECT_ID
   AND curr.CORRNAME = prev.CORRNAME
   AND curr.PLAN_NAME = prev.PLAN_NAME
   AND curr.PRIMAUTH = prev.PRIMAUTH
   AND curr.MAINPACK = prev.MAINPACK
   AND curr.prev_month = prev."MONTH"

ORDER BY
    curr.CONNECT_TYPE,
    curr.CONNECT_ID,
    curr.CORRNAME,
    curr.PLAN_NAME,
    curr.PRIMAUTH,
    curr.MAINPACK,
    curr."MONTH";