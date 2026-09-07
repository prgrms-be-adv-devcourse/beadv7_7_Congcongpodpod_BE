-- 사다리 390과 재측정 390b 사이에 DB 상태가 달라졌는지 본다.
--
-- 배경: 같은 코드로 돌린 두 실행의 p95가 1216ms -> 518ms로 갈렸다.
--   배포 시각으로 코드 차이는 배제됐다 (#474 08-28T16:16Z, #478 08-29T04:26Z 모두 사다리 이전).
--   행 수는 orderId 범위로 +0.2%임을 확인했다. 그러니 여기서 보려는 것은 죽은 튜플과 청소 시점이다.
--
-- 판정 창(UTC):  사다리 390 = 08-29 08:33~08:45,  재측정 390b = 08-29 12:57~13:09
--   last_autovacuum이 08:45~12:57 사이면 그것이 설명이다.
--
-- 읽기 전용. 쓰기도 pg_stat_reset()도 하지 않는다.

SET TIME ZONE 'UTC';


-- 1) 죽은 튜플과 마지막 청소 시각
SELECT relname                AS 테이블,
       n_live_tup             AS 산_행,
       n_dead_tup             AS 죽은_행,
       round(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 1) AS 죽은_퍼센트,
       last_autovacuum,
       last_autoanalyze
FROM pg_stat_user_tables
WHERE relname IN ('orders','order_items','outbox_events','cart_items','carts',
                  'dishes','stores','deposits','deposit_histories','member_snapshots')
ORDER BY n_dead_tup DESC;


-- 2) 두 실행 사이(08:45~12:57 UTC)에 청소가 돌았나
SELECT relname AS 테이블,
       last_autovacuum,
       CASE
         WHEN last_autovacuum IS NULL THEN '한 번도 안 돎'
         WHEN last_autovacuum BETWEEN TIMESTAMPTZ '2026-08-29 08:45+00'
                                  AND TIMESTAMPTZ '2026-08-29 12:57+00'
           THEN '★ 두 실행 사이에 돎 - 이것이 설명'
         WHEN last_autovacuum > TIMESTAMPTZ '2026-08-29 13:09+00' THEN '390b 이후'
         ELSE '사다리 이전 또는 실행 중'
       END AS 판정
FROM pg_stat_user_tables
WHERE relname IN ('orders','order_items','outbox_events','cart_items','dishes')
ORDER BY relname;


-- 3) 테이블 크기 (행 수가 아니라 실제 차지한 용량 - 블로트를 본다)
SELECT relname AS 테이블,
       pg_size_pretty(pg_table_size(relid))          AS 테이블크기,
       pg_size_pretty(pg_indexes_size(relid))        AS 인덱스크기,
       pg_size_pretty(pg_total_relation_size(relid)) AS 합계
FROM pg_stat_user_tables
WHERE relname IN ('orders','order_items','outbox_events','cart_items','dishes','stores')
ORDER BY pg_total_relation_size(relid) DESC;


-- 4) 실제 행 수 (orderId 범위로 추정한 246만이 맞는지)
SELECT (SELECT count(*) FROM orders)        AS orders,
       (SELECT count(*) FROM outbox_events) AS outbox_events,
       (SELECT count(*) FROM cart_items)    AS cart_items;


-- 5) outbox 상태 분포 (정리 정책 #477이 실제로 지우고 있나)
SELECT status                AS 상태,
       count(*)              AS 건수,
       min(occurred_at)      AS 가장_오래된,
       max(occurred_at)      AS 가장_최근
FROM outbox_events
GROUP BY status
ORDER BY count(*) DESC;
