-- V14__add_dishes_store_id_index.sql이 실제로 인덱스를 태우는지 확인한다.
--
-- 왜 필요한가: core-service 테스트는 flyway.enabled=false라(ADR 019) `./gradlew test`가
-- 초록이어도 마이그레이션은 전혀 검증되지 않는다. 게다가 V14는 부분 인덱스라
-- 플래너가 `WHERE is_deleted = false`를 술어로 증명해야만 쓰인다 — 쿼리가 그 조건을
-- 문자 그대로 걸지 않으면 인덱스가 조용히 무시된다.
--
-- **일회용 컨테이너에서 돌린다. 운영 DB에 실행하지 않는다.**
--
--   docker run --rm \
--     -v "$PWD/sql/verify-v14-dishes-index.sql:/v.sql:ro" \
--     -v "$PWD/../../backend/services/core-service/src/main/resources/db/migration:/mig:ro" \
--     -e POSTGRES_PASSWORD=x postgres:16-alpine \
--     sh -c 'docker-entrypoint.sh postgres >/dev/null 2>&1 &
--       for i in $(seq 1 30); do pg_isready -q -U postgres && break; sleep 1; done
--       psql -U postgres -q -f /v.sql 2>&1'
--
-- 판정: 아래 다섯 경로가 모두 "Index Scan using idx_dishes_store_id"여야 한다.
--       하나라도 Seq Scan이면 그 경로는 인덱스를 못 탄다.

\set ON_ERROR_STOP on

-- V4__create_dish_schema.sql의 dishes 정의와 같아야 한다. 스키마가 바뀌면 여기도 맞춘다.
CREATE TABLE public.dishes (
    discount_price numeric(38,2) NOT NULL,
    dish_price numeric(38,2) NOT NULL,
    is_deleted boolean NOT NULL,
    pickup_end_time time(0) without time zone NOT NULL,
    pickup_start_time time(0) without time zone NOT NULL,
    event_version bigint DEFAULT 0 NOT NULL,
    id bigint NOT NULL,
    registered_at timestamp(6) without time zone NOT NULL,
    stock_quantity bigint NOT NULL,
    store_id bigint NOT NULL,
    description character varying(255) NOT NULL,
    category character varying(100),
    dish_name character varying(255) NOT NULL,
    dish_status character varying(255) NOT NULL,
    thumbnail_url character varying(255),
    updated_at timestamp(6) without time zone NOT NULL
);
ALTER TABLE ONLY public.dishes ADD CONSTRAINT dishes_pkey PRIMARY KEY (id);

-- 매장당 50개 = 50,000행. 성장 시나리오다. 20행마다 1건은 삭제 상태로 둬서
-- 부분 인덱스가 실제로 행을 걸러내는지도 함께 본다.
INSERT INTO public.dishes
SELECT 1000, 900, (i % 20 = 0), '20:00', '18:00', 0, i, now(), 10, ((i - 1) / 50) + 1,
       '마감 임박 할인 상품 설명입니다', 'KOREAN', '오늘의 마감 도시락', 'ON_SALE', NULL, now()
FROM generate_series(1, 50000) AS i;
ANALYZE public.dishes;

\echo ''
\echo '=== before: 인덱스 없이 (999건을 버리는 순차 스캔이어야 정상) ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0 WHERE d1_0.store_id = 301 AND d1_0.is_deleted = false;

\i /mig/V14__add_dishes_store_id_index.sql
ANALYZE public.dishes;

\echo ''
\echo '=== after 1/5: findByStoreIdAndIsDeletedFalse — is_deleted = false ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0 WHERE d1_0.store_id = 301 AND d1_0.is_deleted = false;

\echo ''
\echo '=== after 2/5: 부정 형태 NOT is_deleted — 부분 인덱스 술어가 정규화되는지 ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0 WHERE d1_0.store_id = 301 AND NOT d1_0.is_deleted;

\echo ''
\echo '=== after 3/5: findOnSaleByStoreIds — IN + dish_status ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0
WHERE d1_0.store_id IN (301, 302, 303, 304, 305)
  AND d1_0.is_deleted = false
  AND d1_0.dish_status = 'ON_SALE';

\echo ''
\echo '=== after 4/5: findWithLockByStoreIdAndIsDeletedFalse — FOR UPDATE ==='
\echo '    (락을 잡은 채 순차 스캔하면 보유 시간이 행 수에 비례한다)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0 WHERE d1_0.store_id = 301 AND d1_0.is_deleted = false FOR UPDATE;

\echo ''
\echo '=== after 5/5: findAllByStoreIdAndIsDeletedFalseOrderByIdDesc ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM dishes d1_0
WHERE d1_0.store_id = 301 AND d1_0.is_deleted = false
ORDER BY d1_0.id DESC;

\echo ''
\echo '=== 인덱스 크기 ==='
SELECT indexrelname, pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes WHERE relname = 'dishes' ORDER BY indexrelname;
