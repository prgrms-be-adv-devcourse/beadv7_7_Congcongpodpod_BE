-- GET /api/v1/orders 한 요청이 실제로 어떤 SQL을 몇 번 실행하는지 본다.
-- 코드상 2쿼리여야 하는데 실측이 34쿼리라 그 차이를 메우는 문장을 찾는 것이 목적이다.
--
-- 사용법 (psql 세션 하나에서 순서대로):
--   1) 이 파일의 STEP 1을 실행해 스냅샷을 만든다
--   2) 다른 터미널에서 GET /orders?page=0&size=50 을 한 번 보낸다
--   3) STEP 2를 실행해 그 사이 늘어난 호출만 본다
--
-- pg_stat_statements 확장이 필요하다. 없으면 STEP 0이 알려준다.

\echo '=== STEP 0. pg_stat_statements 사용 가능한지 ==='
SELECT
  (SELECT count(*) FROM pg_extension WHERE extname = 'pg_stat_statements') AS 설치됨,
  (SELECT count(*) FROM pg_available_extensions WHERE name = 'pg_stat_statements') AS 설치가능;

\echo ''
\echo '--- 위 설치됨이 0이면 아래는 동작하지 않는다. 대신 app 쪽 SQL 로그를 써야 한다 ---'
\echo ''

\echo '=== STEP 1. 스냅샷 (요청 보내기 전에 실행) ==='
DROP TABLE IF EXISTS qtrace_before;
CREATE TEMP TABLE qtrace_before AS
SELECT queryid, calls, rows, query
FROM pg_stat_statements;
SELECT count(*) AS 스냅샷_문장수 FROM qtrace_before;

\echo ''
\echo '>>> 이제 다른 터미널에서 요청을 한 번 보내세요:'
\echo '>>>   GET /api/v1/orders?page=0&size=50'
\echo '>>> 보낸 뒤 STEP 2(아래 블록)를 실행하세요.'
