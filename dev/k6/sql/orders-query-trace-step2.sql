-- STEP 2 — 요청을 보낸 뒤 실행한다. STEP 1과 같은 psql 세션이어야 한다(임시 테이블 때문).
-- 그 사이 새로 실행된 SQL만 호출 횟수 순으로 보여준다.

\echo '=== 요청 전후로 늘어난 SQL (호출 많은 순) ==='
SELECT
  COALESCE(a.calls - b.calls, a.calls)              AS 늘어난_호출,
  COALESCE(a.rows  - b.rows,  a.rows)               AS 늘어난_행,
  left(regexp_replace(a.query, '\s+', ' ', 'g'), 150) AS 쿼리
FROM pg_stat_statements a
LEFT JOIN qtrace_before b USING (queryid)
WHERE COALESCE(a.calls - b.calls, a.calls) > 0
ORDER BY 늘어난_호출 DESC
LIMIT 25;

\echo ''
\echo '=== 합계 ==='
SELECT
  sum(COALESCE(a.calls - b.calls, a.calls)) AS 총_실행문장수,
  count(*)                                  AS 서로_다른_문장수
FROM pg_stat_statements a
LEFT JOIN qtrace_before b USING (queryid)
WHERE COALESCE(a.calls - b.calls, a.calls) > 0;

\echo ''
\echo '읽는 법:'
\echo '  - orders 를 select 하는 문장이 1~2회      → 정상(페이지 + count)'
\echo '  - 같은 문장이 30여 회 반복                → 그 문장이 N+1의 정체다'
\echo '  - stores/dishes/members 를 도는 문장이 있다면 → 주문마다 딸린 조회가 있다는 뜻'
