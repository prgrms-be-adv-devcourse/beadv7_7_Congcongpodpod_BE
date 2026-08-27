#!/usr/bin/env bash
#
# 부하 실행 중 DB 행 락 대기를 1초 간격으로 표본 수집한다.
#
# 1라운드 집중도 A/B(weighted)에서 주문 p95가 3.1배로 늘었지만, 부하 중 실시간 잠금 대기를
# 수집하지 못해 "Dish 행 락이 원인이다"를 직접 증명하지 못했다(정황 증거만 남음). 이 스크립트는
# 그 구멍을 메운다 — 어느 테이블에서, 얼마나 오래, 무엇이 무엇을 막았는지를 표본으로 남긴다.
#
# 사용법 (weighted 실행과 동시에, 별도 터미널에서. DB 서버로 가는 SSH 터널을 먼저 열어 둔다):
#   ssh -L 5433:10.30.2.93:5433 lastdish-data   # 별도 터미널, 계속 열어 둠
#   DB_HOST=host.docker.internal DB_PORT=5433 DB_USER=core DB_NAME=core_db \
#     DB_PASSWORD=... ./monitor-db-locks.sh
#
# 환경변수:
#   DB_HOST            필수. 기본값 없음 — 잘못 추측한 기본값으로 조용히 딴 곳을 재는 것보다
#                       실패가 낫다. macOS에서 Mac 자신에 뚫은 SSH 터널에 붙으려면
#                       host.docker.internal을 쓴다(localhost는 컨테이너 자신을 가리킨다 —
#                       실측 확인됨).
#   DB_PASSWORD         필수.
#   DB_PORT             기본 5432
#   DB_USER             기본 postgres
#   DB_NAME             기본 lastdish
#   DURATION_SECONDS    기본 900 (15분 — warm-up 5분 + 측정 10분 여유)
#   INTERVAL_SECONDS     기본 1
#   OUTPUT_FILE          기본 ./results/db-locks-<타임스탬프>.csv
#   PGTZ                기본 Asia/Seoul — psql 세션 시간대. 아래 참고
#
# 이전 버전은 CONTAINER 환경변수로 "같은 서버에 이미 떠 있는 postgres 컨테이너 안에
# docker exec로 들어가는" 방식이었다. 실제 운영 DB는 어느 서버에도 컨테이너로 떠 있지 않고
# 별도 호스트에 있어서(kubectl get pods -A, docker ps 둘 다 postgres 없음 — 확인함) 그
# 방식은 애초에 접속할 대상이 없었다. 이 버전은 SSH 터널로 열어 둔 포트에 네트워크로 접속한다.
#
# 매초 `docker run`으로 컨테이너를 새로 띄우면 기동 오버헤드(실측 234~258ms)가 쌓여 900회
# 반복 시 표본 간격이 크게 밀린다. 그래서 psql 클라이언트 컨테이너를 스크립트 시작 시
# 한 번만 띄워 두고(sleep infinity), 매초는 그 안에서 docker exec만 한다 — 컨테이너
# 재생성 없이 새 psql 연결만 매번 새로 연다. 그래도 docker exec 자체와 새 psql 접속에
# 오버헤드가 남아 실측 표본 간격은 1.0초가 아니라 약 1.09~1.10초다(로컬 검증). 즉
# DURATION_SECONDS=900을 요청해도 실제 표본 수는 900개가 아니라 820~830개 근처가
# 된다 — 표본이 잘못된 게 아니라 더 적을 뿐이니, 분모(N)는 항상 실제 표본 수(하트비트
# 포함 CSV 행 수)로 계산한다.
#
# 비밀번호는 `-e PGPASSWORD=값` 형태로 넘기지 않는다. 이 형태는 ps -Ao args에 평문으로
# 그대로 뜬다(같은 머신의 다른 사용자에게도 보임, 15분 실행이면 900번 반복 노출). 대신 이
# 스크립트 자신의 환경에 PGPASSWORD를 export해 두고 docker에는 `-e PGPASSWORD`(값 없이
# 변수명만)로 넘긴다 — docker가 자기 프로세스 환경에서 값을 읽어 컨테이너로 전달하므로
# argv에는 변수명만 남는다.
#
# 실행기록 2026-08-25 §7.5의 실패 원인(호스트 셸이 컨테이너 환경변수를 먼저 빈 문자열로
# 치환)을 피하려고, 컨테이너 셸에게 변수 해석을 맡기지 않는다. 접속 정보는 전부 호스트 쪽
# 인자로 psql에 직접 넘긴다.
#
# 로컬 postgres:16 컨테이너에 실제로 행 락 경합을 만들어 검증했다(운영 컨테이너는 아님).
# 그 과정에서 애초 설계와 다른 점을 하나 발견해 고쳤다: Hibernate의 findWithLockBy...가
# 쓰는 `SELECT ... FOR UPDATE` 방식의 행 락 대기는 pg_locks에 locktype=transactionid로
# 잡히고 relation이 비어 있다. 그래서 대기 중인 pid가 "동시에 허가받아 쥐고 있는" relation
# 락에서 테이블명을 가져오도록 LATERAL 조인을 추가했다. 이 보정 없이 짰다면 우리가 가장
# 보고 싶은 Dish·Order·Deposit 행 락 대기가 전부 table_name=unknown으로 나왔을 것이다.
#
# 대기가 0건인 초에도 한 행을 남긴다(table_name=none). 이게 없으면 "대기가 없었다"와
# "접속이 끊겨 수집이 죽었다"가 CSV만으로 구분되지 않고, 대기율 같은 판정 지표의 분모도
# 이 행이 있어야 파일에서 직접 셀 수 있다. 항상 최소 1행을 만드는 더미 행에
# LEFT JOIN해서 보장한다.
#
# psql 컨테이너의 기본 시간대는 UTC라 sampled_at이 UTC로 찍힌다. 짝을 이루는
# monitor-hikari.sh는 curl을 실행하는 로컬(대개 KST) 시각을 쓰므로, 그대로 두면 두 CSV를
# 나란히 대조할 때 9시간이 어긋난다(실측: KST 19:05 vs 컨테이너 10:05). PGTZ로 세션
# 시간대를 맞춘다.
#
# 첫 실행은 짧게(예: DURATION_SECONDS=10) 돌려서 CSV가 채워지는지 먼저 확인한다.

set -euo pipefail

if [ -z "${DB_HOST:-}" ]; then
  echo "DB_HOST가 필요합니다. 예: DB_HOST=host.docker.internal DB_PORT=5433 DB_USER=core DB_NAME=core_db DB_PASSWORD=... $0" >&2
  exit 2
fi
if [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_PASSWORD가 필요합니다." >&2
  exit 2
fi

DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-lastdish}"
DURATION_SECONDS="${DURATION_SECONDS:-900}"
INTERVAL_SECONDS="${INTERVAL_SECONDS:-1}"
PGTZ="${PGTZ:-Asia/Seoul}"

# docker exec -e PGPASSWORD(값 없이)가 이 값을 읽어가도록 이 스크립트 자신의 환경에 둔다.
# argv에는 안 실리지만, 이 프로세스의 환경변수 자체는 같은 사용자·root가 /proc으로 볼 수
# 있다 — ps 평문 노출보다는 낫지만 완전한 격리는 아니다.
export PGPASSWORD="$DB_PASSWORD"

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
results_dir="$script_dir/results"
mkdir -p "$results_dir"
OUTPUT_FILE="${OUTPUT_FILE:-$results_dir/db-locks-$(date +%Y%m%d-%H%M%S).csv}"

CLIENT_CONTAINER="monitor-db-locks-$$"

cleanup() {
  docker rm -f "$CLIENT_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

echo "psql 클라이언트 컨테이너 시작 — $CLIENT_CONTAINER"
docker run -d --name "$CLIENT_CONTAINER" postgres:16-alpine sleep infinity >/dev/null

# 대기 중인 락(granted=false)마다 한 행. 가장 최근 사용된 waiting_query가 nullable일 수
# 있으니 COALESCE로 빈 문자열 대신 명시적 표시를 남긴다.
#
# 어느 테이블에서 대기가 걸렸는지(table_name), 대기 쪽과 막는 쪽 각각의 SQL 앞부분으로
# "주문 트랜잭션인지 CartDishStateSynchronizer(Inbox 소비자)인지"를 구분한다 — 주문 경로는
# 단일 PK 조건(where id=)으로 dishes를 잠그고, Inbox 소비자는 findAllByDishId라 다른
# 조건(where dish_id=)으로 cart_items를 잠근다.
#
# 필드 구분자로 콤마 대신 파이프(|)를 쓴다. blocking_pids는 배열이라 콤마를 포함할 수
# 있고(예: {12,34}), waiting_query에도 콤마가 흔해 콤마 구분자를 쓰면 뒤 컬럼이 밀린다.
#
# w.relation만으로는 테이블을 못 찾는 경우가 실제로 더 흔하다. `SELECT ... FOR UPDATE`로
# 만드는 행 락 대기(Hibernate의 findWithLockBy...가 쓰는 방식과 동일)는 pg_locks에서
# locktype=transactionid로 잡히고 relation이 NULL이다 — 로컬 postgres로 재현해 직접
# 확인했다. 실제 테이블명은 같은 pid가 "동시에 허가받아 쥐고 있는" relation 락에서
# 가져와야 한다.
#
# (SELECT 1) heartbeat에 LEFT JOIN해서 대기가 0건이어도 최소 1행(table_name=none)을
# 보장한다.
READ_ONLY_QUERY="
WITH waits AS (
  SELECT
    w.pid AS waiting_pid,
    COALESCE(w.relation::regclass::text, held_table.relname, 'unknown') AS table_name,
    w.mode AS lock_mode,
    EXTRACT(MILLISECONDS FROM (clock_timestamp() - a.query_start))::bigint AS wait_ms,
    regexp_replace(COALESCE(a.query, ''), E'[\\n\\r]+', ' ', 'g') AS waiting_query,
    array_to_string(pg_blocking_pids(w.pid), ';') AS blocking_pids
  FROM pg_locks w
  JOIN pg_stat_activity a ON a.pid = w.pid
  LEFT JOIN LATERAL (
    SELECT c.relname
    FROM pg_locks held
    JOIN pg_class c ON c.oid = held.relation
    WHERE held.pid = w.pid
      AND held.granted
      AND held.locktype = 'relation'
      AND c.relkind = 'r'
    LIMIT 1
  ) held_table ON true
  WHERE NOT w.granted
)
SELECT
  to_char(clock_timestamp(), 'YYYY-MM-DD HH24:MI:SS.MS') AS sampled_at,
  COALESCE(waits.waiting_pid::text, '') AS waiting_pid,
  COALESCE(waits.table_name, 'none') AS table_name,
  COALESCE(waits.lock_mode, '') AS lock_mode,
  COALESCE(waits.wait_ms, 0) AS wait_ms,
  COALESCE(waits.waiting_query, '') AS waiting_query,
  COALESCE(waits.blocking_pids, '') AS blocking_pids
FROM (SELECT 1) heartbeat
LEFT JOIN waits ON true;
"

# 헤더는 위 SELECT의 컬럼 순서와 반드시 같아야 한다. 순서가 어긋나면 wait_ms 자리에
# pid가, lock_mode 자리에 테이블명이 들어가는 식으로 완전히 잘못 읽힌다.
echo "sampled_at|waiting_pid|table_name|lock_mode|wait_ms|waiting_query|blocking_pids" > "$OUTPUT_FILE"

echo "DB 락 대기 수집 시작 — $DB_HOST:$DB_PORT/$DB_NAME, ${DURATION_SECONDS}초 동안 ${INTERVAL_SECONDS}초 간격"
echo "결과 파일: $OUTPUT_FILE"

end_at=$(($(date +%s) + DURATION_SECONDS))

while [ "$(date +%s)" -lt "$end_at" ]; do
  # -t: 헤더 생략. -A: 정렬 없이 필드 구분자만. 컨테이너를 매번 새로 만들지 않고 이미 떠
  # 있는 클라이언트 컨테이너 안에서 새 psql 연결만 연다.
  docker exec -e PGPASSWORD -e PGTZ="$PGTZ" "$CLIENT_CONTAINER" \
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -F'|' -c "$READ_ONLY_QUERY" \
    >> "$OUTPUT_FILE" 2>>"$OUTPUT_FILE.err" || true
  sleep "$INTERVAL_SECONDS"
done

echo "수집 종료. $(wc -l < "$OUTPUT_FILE") 행 기록됨 (헤더 포함)."
if [ -s "$OUTPUT_FILE.err" ]; then
  echo "경고: $OUTPUT_FILE.err 에 에러 로그가 있다. 접속 실패가 조용히 반복됐을 수 있으니 확인한다."
fi
