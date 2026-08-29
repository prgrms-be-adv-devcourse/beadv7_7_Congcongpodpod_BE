-- outbox_events의 순차 스캔 2,460만 행이 어디서 나왔는지 좁힌다. **읽기 전용**이다.
--
-- 배경: 2026-08-29 배포 후 조사에서 outbox_events가 순차 스캔으로 가장 많은 행을 읽는
-- 테이블로 나왔다(24,600,000행). claim 쿼리를 의심했으나 로컬 재현에서 기각됐다 —
-- PostgreSQL이 OR를 BitmapOr로 쪼개 양쪽 다 인덱스를 탄다(6버퍼).
-- 그래서 원인을 다시 찾는다.

\echo '=== 1) 통계가 언제부터 쌓인 것인가 ==='
\echo '    (DB가 다시 시드된 것으로 보이는데, 통계는 TRUNCATE로 초기화되지 않는다.'
\echo '     그래서 이 값들은 시드 이전 기간까지 포함할 수 있다)'
SELECT datname AS "DB", stats_reset AS "통계 초기화 시각",
       now() - stats_reset AS "누적 기간"
FROM pg_stat_database WHERE datname = current_database();

\echo ''
\echo '=== 2) 지금 상태 분포 — PUBLISHED가 대부분이면 정리 부재가 확인된다 ==='
SELECT status AS "상태", count(*) AS "건수",
       min(occurred_at) AS "가장 오래된", max(occurred_at) AS "가장 최근"
FROM outbox_events GROUP BY status ORDER BY count(*) DESC;

\echo ''
\echo '=== 3) claim 쿼리가 지금 어떤 계획을 쓰나 ==='
\echo '    Bitmap Index Scan이면 claim은 범인이 아니다. Seq Scan이면 범인이다.'
EXPLAIN (ANALYZE, BUFFERS)
SELECT event_id FROM outbox_events
 WHERE (status = 'PENDING' OR (status = 'PROCESSING' AND locked_at < now()))
 ORDER BY occurred_at
 LIMIT 100;

\echo ''
\echo '=== 4) 인덱스가 담고 있는 것 vs 실제로 찾는 것 ==='
\echo '    claim은 PENDING/PROCESSING만 찾는데 인덱스는 PUBLISHED까지 전부 담는다.'
SELECT
    pg_size_pretty(pg_relation_size('idx_outbox_status_occurred_at')) AS "현재 인덱스 크기",
    count(*) FILTER (WHERE status IN ('PENDING','PROCESSING'))        AS "실제로 찾는 행",
    count(*)                                                          AS "인덱스가 담는 행",
    round(100.0 * count(*) FILTER (WHERE status IN ('PENDING','PROCESSING'))
          / nullif(count(*), 0), 4)                                   AS "쓸모있는 비율(%)"
FROM outbox_events;

\echo ''
\echo '=== 5) outbox_events 스캔 통계 (1번의 누적 기간 기준) ==='
SELECT seq_scan AS "순차스캔", seq_tup_read AS "순차로읽은행",
       CASE WHEN seq_scan > 0 THEN seq_tup_read / seq_scan END AS "스캔당평균행",
       idx_scan AS "인덱스스캔", n_live_tup AS "현재행수",
       n_tup_ins AS "누적INSERT", n_tup_del AS "누적DELETE"
FROM pg_stat_user_tables WHERE relname = 'outbox_events';

\echo ''
\echo '=== 6) 인덱스별 사용 횟수 ==='
SELECT indexrelname AS "인덱스", idx_scan AS "사용횟수",
       pg_size_pretty(pg_relation_size(indexrelid)) AS "크기"
FROM pg_stat_user_indexes WHERE relname = 'outbox_events';
