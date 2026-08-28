-- V14 배포 후 인덱스가 실제로 쓰이는지 확인한다. **읽기 전용**이다.
--
--   docker run --rm -i -e PGPASSWORD="$DB_PASSWORD" -v "$PWD/sql:/sql:ro" postgres:16-alpine \
--     psql -h host.docker.internal -p 5433 -U core -d core_db -f /sql/verify-after-deploy.sql

\echo '=== 1) V14 마이그레이션이 적용됐나 ==='
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 3;

\echo ''
\echo '=== 2) 인덱스가 실제로 만들어졌나 ==='
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'dishes';

\echo ''
\echo '=== 3) 플래너가 인덱스를 고르나 (Index Scan이어야 성공) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM dishes d1_0 WHERE d1_0.store_id = 301 AND d1_0.is_deleted = false;

\echo ''
\echo '=== 4) 스캔 통계 스냅샷 — 이 값을 적어 두고 부하 뒤에 다시 잰다 ==='
\echo '    판정: 부하 전후로 seq_scan 증가폭이 idx_scan 증가폭보다 훨씬 작아야 한다'
SELECT
    relname                          AS "테이블",
    seq_scan                         AS "순차스캔",
    seq_tup_read                     AS "순차로읽은행",
    idx_scan                         AS "인덱스스캔",
    n_live_tup                       AS "현재행수",
    pg_size_pretty(pg_relation_size(relid)) AS "크기"
FROM pg_stat_user_tables
WHERE relname IN ('dishes', 'stores', 'orders', 'cart_items', 'outbox_events')
ORDER BY seq_tup_read DESC;

\echo ''
\echo '=== 5) 인덱스별 사용 횟수 ==='
SELECT relname AS "테이블", indexrelname AS "인덱스", idx_scan AS "사용횟수",
       pg_size_pretty(pg_relation_size(indexrelid)) AS "크기"
FROM pg_stat_user_indexes
WHERE relname IN ('dishes', 'stores', 'orders')
ORDER BY relname, idx_scan DESC;
