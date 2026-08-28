-- dishes를 store_id로 찾는 경로에 인덱스가 없어 매번 전체 순차 스캔이 일어난다.
--
-- 실측(2026-08-28, pg_stat_user_tables): 순차 스캔 552,390회로 누적 5.5억 행을 읽었다.
-- dishes 전체 조회 615,043회 중 90%가 순차 스캔이고, 매 회 1건을 찾으려고 999건을 버렸다.
-- 지금은 1,000행이라 건당 0.25 ms지만 매장당 상품이 늘면 스캔 비용만 선형으로 증가한다.
--
-- 지금 만드는 이유: 테이블이 작을 때 인덱스 빌드가 즉시 끝난다. 커진 뒤에는 CREATE INDEX가
-- 쓰기를 막고, 피하려면 CONCURRENTLY라 실패 시 INVALID 인덱스 정리가 따라온다.
-- 플래너는 테이블이 작으면 인덱스가 있어도 쓰지 않으므로, 미리 두면 전환이 무중단으로 일어난다.
--
-- store_id로 조회하는 경로는 DishJpaRepository에 6개이고 전부 is_deleted = false를 함께 건다.
-- 부분 인덱스로 두면 삭제된 상품을 제외해 인덱스가 더 작아진다.
CREATE INDEX idx_dishes_store_id
    ON public.dishes USING btree (store_id)
    WHERE is_deleted = false;
