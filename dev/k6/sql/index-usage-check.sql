-- 인덱스 부재가 실제로 순차 스캔을 유발하고 있는지 확인한다. 읽기 전용이다.
-- 2026-08-28 집중도 A/B 6회차 직후 실행하면 통계가 신선하다.

\echo '=== 1. 테이블별 순차 스캔 vs 인덱스 스캔 ==='
SELECT relname                                        AS 테이블,
       n_live_tup                                     AS 현재행수,
       seq_scan                                       AS 순차스캔횟수,
       seq_tup_read                                   AS 순차로읽은행,
       idx_scan                                       AS 인덱스스캔횟수,
       (seq_tup_read / NULLIF(seq_scan, 0))::bigint   AS 스캔당평균행,
       pg_size_pretty(pg_relation_size(relid))        AS 테이블크기
FROM pg_stat_user_tables
WHERE relname IN ('dishes', 'cart_items', 'stores', 'orders', 'carts')
ORDER BY seq_tup_read DESC;

\echo ''
\echo '=== 2. 현재 걸려 있는 인덱스 목록 ==='
SELECT tablename AS 테이블, indexname AS 인덱스, indexdef AS 정의
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('dishes', 'cart_items', 'stores', 'orders', 'carts')
ORDER BY tablename, indexname;

\echo ''
\echo '=== 3. 실행 계획 — dishes를 store_id로 조회 (인덱스 없음 가설) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM dishes WHERE store_id = 301 AND is_deleted = false;

\echo ''
\echo '=== 4. 실행 계획 — cart_items를 dish_id로 조회 (락 경로, 인덱스 없음 가설) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM cart_items WHERE dish_id = 301;

\echo ''
\echo '=== 5. 실행 계획 — orders를 member_id로 조회 (인덱스 있음, 대조군) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders
WHERE member_id = 1 AND is_deleted = false
ORDER BY created_at DESC
LIMIT 50;

\echo ''
\echo '=== 6. cart_items 실제 행 수 (UNIQUE(cart_id) 제약 영향 확인) ==='
SELECT (SELECT count(*) FROM cart_items) AS cart_items_행수,
       (SELECT count(*) FROM carts)      AS carts_행수,
       (SELECT count(*) FROM dishes)     AS dishes_행수,
       (SELECT count(*) FROM stores)     AS stores_행수,
       (SELECT count(*) FROM orders)     AS orders_행수;
