#!/usr/bin/env bash
#
# 집중도 A/B 한 번(한 조건)을 끝까지 실행한다 — 수집기 2개와 k6를 시각을 맞춰 띄우고,
# 끝난 뒤 결과 파일이 쓸 만한지까지 검사한다.
#
# 왜 스크립트로 묶었나: 이 실행은 터미널 3개(락 수집·Hikari 수집·k6)를 정확한 순서와
# 간격으로 띄워야 하고, 끝나면 매번 같은 검사를 반복해야 한다. 6~7회 반복하는 동안 손으로
# 하면 라벨 오타·시작 시각 기록 누락·수집기 실패를 놓치기 쉽다. 1라운드에서 실시간 락
# 감시가 26분간 조용히 실패한 적이 있다(실행기록 §7.5).
#
# 전체 세션(A B B A A B)을 통째로 자동화하지는 않는다. 계획서 §10이 "실행 중 자리를
# 비우지 않는다"와 수동 중단 기준을 두고 있어서, 실행 사이에 사람이 판단할 자리를 남긴다.
# 이 스크립트는 한 번만 돌리고, 결과를 보고 다음 실행 여부를 사람이 정한다.
#
# 사용법:
#   1) 터널 3개를 먼저 띄운다(각각 별도 터미널, 세션 내내 열어 둠)
#        ssh -L 5433:10.30.2.93:5433 lastdish-data
#        ssh lastdish   그리고 그 안에서: kubectl port-forward svc/core-service -n app 8081:80
#        ssh -L 8081:localhost:8081 lastdish
#   2) DB 비밀번호를 환경변수로 준비한다(셸 히스토리에 남기지 않으려면 read -s 사용)
#        read -rs DB_PASSWORD && export DB_PASSWORD
#   3) 실행
#        ./run-concentration-ab.sh uniform  1
#        ./run-concentration-ab.sh weighted 1
#
# 인자:
#   $1  분포   uniform | weighted
#   $2  회차   1, 2, 3 ... (라벨에 붙는다). warmup을 쓰려면 회차 자리에 warmup을 넣는다
#
# 환경변수:
#   DB_PASSWORD      필수. core_db 접속 비밀번호
#   DB_HOST          기본 host.docker.internal (macOS에서 Mac 자신의 터널에 붙는 주소)
#   DB_PORT          기본 5433
#   ACTUATOR_URL     기본 http://localhost:8081/actuator/prometheus
#   ORDER_RATE       기본 120 (계획서 §10.1 — 포화점 180의 67%. 올리지 않는다)
#   PREFLIGHT_ONLY   1이면 사전 점검만 수행하고 수집기·k6는 시작하지 않는다
#
# 시각: 두 수집기를 모두 UTC로 찍는다(TZ=UTC, PGTZ=UTC). 락 CSV는 DB 서버 시계,
# Hikari CSV는 이 Mac 시계라 출처가 다르므로, 같은 타임존으로 맞춰야 나란히 대조할 수
# 있다. 자세한 배경은 계획서 §9.1.

set -euo pipefail

DIST="${1:-}"
RUN_NO="${2:-}"

if [ "$DIST" != "uniform" ] && [ "$DIST" != "weighted" ]; then
  echo "사용법: $0 <uniform|weighted> <회차>" >&2
  echo "  예: $0 uniform 1" >&2
  exit 2
fi
if [ -z "$RUN_NO" ]; then
  echo "회차를 지정하세요(1, 2, 3 또는 warmup). 예: $0 $DIST 1" >&2
  exit 2
fi
if [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_PASSWORD가 필요합니다. 예: read -rs DB_PASSWORD && export DB_PASSWORD" >&2
  exit 2
fi

DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-5433}"
ACTUATOR_URL="${ACTUATOR_URL:-http://localhost:8081/actuator/prometheus}"
ORDER_RATE="${ORDER_RATE:-120}"
SEED_ACCOUNT_COUNT="${SEED_ACCOUNT_COUNT:-1000}"
SEED_ACCOUNT_WIDTH="${SEED_ACCOUNT_WIDTH:-4}"

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
results_dir="$script_dir/results"
mkdir -p "$results_dir"

LABEL="r2-conc-${DIST}-${RUN_NO}"
LOCKS_CSV="$results_dir/locks-${LABEL}.csv"
HIKARI_CSV="$results_dir/hikari-${LABEL}.csv"
K6_LOG="$results_dir/${LABEL}-k6.log"
RUN_LOG="$results_dir/r2-conc-run-log.txt"

# 수집기는 k6보다 30초 먼저 시작해 유휴 기준선을 잡고, k6(워밍업 2분 + 측정 10분 = 12분)
# 보다 넉넉히 뒤까지 돈다. 30 + 720 + 90(여유) = 840초.
COLLECT_SECONDS=840
K6_LEAD_SECONDS=30

echo "=========================================="
echo " 집중도 A/B 실행 — $LABEL"
echo "=========================================="
echo "  분포        : $DIST"
echo "  주문 도착률 : ${ORDER_RATE}건/분"
echo "  수집        : ${COLLECT_SECONDS}초 (k6보다 ${K6_LEAD_SECONDS}초 먼저 시작)"
echo

# ── 사전 점검 ─────────────────────────────────────────────
# 14분 뒤에 "접속이 안 됐네"를 발견하지 않도록 여기서 먼저 끊는다.
echo "[사전 점검]"

if [ ! -f "$script_dir/.env" ]; then
  echo "  ✗ dev/k6/.env가 없습니다." >&2
  exit 2
fi
echo "  ✓ .env 있음"

set -a
# shellcheck disable=SC1091
source "$script_dir/.env"
set +a
if [ -z "${SEED_PASSWORD:-}" ]; then
  echo "  ✗ .env에 SEED_PASSWORD가 없습니다." >&2
  exit 2
fi

# 응답이 수백 KB라 `curl | grep -q` 형태로 쓰면 grep이 첫 매치에서 파이프를 닫아
# curl이 쓰기 실패(exit 23)한다. 본문을 한 번만 받아 변수에 담고 거기서 찾는다.
ACTUATOR_BODY="$(curl -fsS --max-time 5 "$ACTUATOR_URL" 2>/dev/null || true)"
POOL_MAX="$(printf '%s\n' "$ACTUATOR_BODY" | grep '^hikaricp_connections_max' | head -1 | awk '{print $NF}' || true)"
if [ -z "$POOL_MAX" ]; then
  echo "  ✗ Hikari 지표를 못 읽습니다: $ACTUATOR_URL" >&2
  echo "    kubectl port-forward와 8081 터널이 살아 있는지 확인하세요." >&2
  exit 2
fi
echo "  ✓ Hikari 도달 가능 (풀 상한 $POOL_MAX)"

if ! PGPASSWORD="$DB_PASSWORD" docker run --rm -i -e PGPASSWORD postgres:16-alpine \
     psql -h "$DB_HOST" -p "$DB_PORT" -U core -d core_db -tAc "select 1" >/dev/null 2>&1; then
  echo "  ✗ DB에 접속할 수 없습니다: $DB_HOST:$DB_PORT" >&2
  echo "    ssh -L 5433:10.30.2.93:5433 lastdish-data 터널이 살아 있는지 확인하세요." >&2
  exit 2
fi
echo "  ✓ DB 도달 가능 ($DB_HOST:$DB_PORT/core_db)"

STATE_HOST_FILE="$results_dir/r2-concentration-state.json"
DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_PASSWORD="$DB_PASSWORD" \
  SEED_ACCOUNT_WIDTH="$SEED_ACCOUNT_WIDTH" \
  "$script_dir/prepare-concentration-state.sh" "$STATE_HOST_FILE" >/dev/null
echo "  ✓ 현재 배포 데이터로 상태 파일 재생성 (판매자 301~340)"

login_check() {
  local email="$1" password="$2" label="$3" body
  body="$(curl -fsS --max-time 10 -X POST "${BASE_URL%/}/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" 2>/dev/null || true)"
  if ! printf '%s' "$body" | jq -e '.data.accessToken | strings | length > 0' >/dev/null 2>&1; then
    echo "  ✗ $label 로그인 실패: $email" >&2
    return 1
  fi
  echo "  ✓ $label 로그인 가능 ($email)"
}

login_check "seller0001@seed.lastdish.kr" "$SEED_PASSWORD" "구매자"
login_check "seller0301@seed.lastdish.kr" "$SEED_PASSWORD" "판매자"

if [ -e "$LOCKS_CSV" ] || [ -e "$HIKARI_CSV" ] || [ -e "$K6_LOG" ]; then
  if [ "${ARCHIVE_EXISTING:-0}" = "1" ]; then
    archive_dir="$results_dir/archive-$(date -u '+%Y%m%d-%H%M%S')-${LABEL}"
    mkdir -p "$archive_dir"
    mv "$LOCKS_CSV" "$LOCKS_CSV.err" "$HIKARI_CSV" "$HIKARI_CSV.err" "$K6_LOG" \
      "$results_dir/${LABEL}-locks.stdout" "$results_dir/${LABEL}-hikari.stdout" \
      "$archive_dir/" 2>/dev/null || true
    echo "  ↻ 기존 결과 파일을 보관함: results/$(basename "$archive_dir")/"
  else
    echo "  ✗ 같은 라벨의 결과 파일이 이미 있습니다: $LABEL" >&2
    echo "    덮어쓰지 않습니다. 회차를 바꾸거나 기존 파일을 옮기세요." >&2
    echo "    (지난 실패분을 자동으로 보관하고 재실행하려면 ARCHIVE_EXISTING=1을 붙이세요.)" >&2
    exit 2
  fi
fi
echo "  ✓ 라벨 충돌 없음"
echo

if [ "${PREFLIGHT_ONLY:-0}" = "1" ]; then
  echo "✓ 사전 점검만 완료했습니다. 수집기와 k6는 시작하지 않았습니다."
  exit 0
fi

# ── 수집기 시작 ───────────────────────────────────────────
echo "[수집기 시작]"

TZ=UTC PGTZ=UTC \
  DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_USER=core DB_NAME=core_db \
  DB_PASSWORD="$DB_PASSWORD" \
  DURATION_SECONDS="$COLLECT_SECONDS" OUTPUT_FILE="$LOCKS_CSV" \
  "$script_dir/monitor-db-locks.sh" > "$results_dir/${LABEL}-locks.stdout" 2>&1 &
LOCKS_PID=$!
echo "  락 수집 시작 (pid $LOCKS_PID)"

TZ=UTC \
  TARGET_URL="$ACTUATOR_URL" \
  DURATION_SECONDS="$COLLECT_SECONDS" OUTPUT_FILE="$HIKARI_CSV" \
  "$script_dir/monitor-hikari.sh" > "$results_dir/${LABEL}-hikari.stdout" 2>&1 &
HIKARI_PID=$!
echo "  Hikari 수집 시작 (pid $HIKARI_PID)"

# 어떤 경로로 빠져나가도 수집기를 남기지 않는다.
cleanup() {
  kill "$LOCKS_PID" "$HIKARI_PID" 2>/dev/null || true
}
trap cleanup INT TERM

echo "  유휴 기준선 확보를 위해 ${K6_LEAD_SECONDS}초 대기..."
sleep "$K6_LEAD_SECONDS"
echo

# ── k6 실행 ───────────────────────────────────────────────
# 측정 창 계산에 쓰이므로 시작 시각을 반드시 남긴다. k6 요약에는 종료 시각만 있다.
K6_START_UTC="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "run=$RUN_NO cond=$DIST label=$LABEL k6_start_utc=$K6_START_UTC locks=$(basename "$LOCKS_CSV") hikari=$(basename "$HIKARI_CSV")" >> "$RUN_LOG"

echo "[k6 실행] 시작 $K6_START_UTC — 워밍업 2분 + 측정 10분"
echo "  측정 창: $K6_START_UTC +2분 ~ +12분"
echo "  콘솔 로그: $(basename "$K6_LOG")"
echo

cd "$script_dir"
# k6 콘솔(auth_login 등 실패 상세를 찍는 console.error 포함)을 파일로도 남긴다.
# pipefail이 켜져 있어(파일 상단 set -euo pipefail) tee를 거쳐도 k6의 종료 코드가 그대로 전달된다.
./k6.sh capacity-ladder \
  -e ORDER_RATE="$ORDER_RATE" \
  -e TARGET_DISTRIBUTION="$DIST" \
  -e SEED_ACCOUNT_COUNT="$SEED_ACCOUNT_COUNT" \
  -e LOADTEST_PASSWORD="$SEED_PASSWORD" \
  -e STATE_FILE=/scripts/results/r2-concentration-state.json \
  -e CAMPAIGN_DAY=1 \
  -e DATASET_EPOCH=deployed-seed-current \
  -e RESULT_LABEL="$LABEL" 2>&1 | tee "$K6_LOG" \
  || echo "  ⚠ k6가 0이 아닌 코드로 끝났습니다(임계값 위반이면 정상 동작일 수 있음)"

echo
echo "[수집기 종료 대기]"
wait "$LOCKS_PID" 2>/dev/null || true
wait "$HIKARI_PID" 2>/dev/null || true
trap - INT TERM
echo "  수집 완료"
echo

# ── 결과 검사 ─────────────────────────────────────────────
# 계획서 §5.1 — 반쪽 데이터로 판정하지 않는다. 여기서 걸리면 그 쌍은 폐기 후보다.
echo "[결과 검사]"
PROBLEM=0

locks_rows=$(( $(wc -l < "$LOCKS_CSV") - 1 ))
hikari_rows=$(( $(wc -l < "$HIKARI_CSV") - 1 ))
echo "  락 CSV     : ${locks_rows}행"
echo "  Hikari CSV : ${hikari_rows}행"

# 락 수집은 docker exec 오버헤드로 표본 간격이 약 1.09초라 840초에 ~770행이 정상이다.
if [ "$locks_rows" -lt 600 ]; then
  echo "  ✗ 락 CSV 행이 너무 적습니다(수집이 중간에 죽었을 수 있음)"; PROBLEM=1
fi
# Hikari는 curl 왕복 때문에 표본 간격이 실측 1.37~1.43초라 840초에 589~611행이 정상이다
# (2026-08-28 3회 실측). 부하가 무거운 weighted에서 간격이 더 벌어져 589까지 내려간다.
# 수집이 실제로 죽었는지는 행 수보다 .err 파일과 ERROR 행이 훨씬 정확한 신호다.
if [ "$hikari_rows" -lt 500 ]; then
  echo "  ✗ Hikari CSV 행이 너무 적습니다(수집이 중간에 죽었을 수 있음)"; PROBLEM=1
fi

for f in "$LOCKS_CSV.err" "$HIKARI_CSV.err"; do
  if [ -s "$f" ]; then
    echo "  ✗ 에러 로그가 비어 있지 않습니다: $(basename "$f")"
    head -3 "$f" | sed 's/^/      /'
    PROBLEM=1
  fi
done

err_rows=$(grep -c '|ERROR|' "$HIKARI_CSV" 2>/dev/null || true)
if [ "${err_rows:-0}" -gt 0 ]; then
  echo "  ✗ Hikari CSV에 ERROR 행 ${err_rows}개(스크랩 실패)"; PROBLEM=1
fi

unknown_rows=$(awk -F'|' 'NR>1 && $3=="unknown"' "$LOCKS_CSV" 2>/dev/null | wc -l | tr -d ' ')
if [ "${unknown_rows:-0}" -gt 0 ]; then
  echo "  ⚠ table_name=unknown 행 ${unknown_rows}개 — 많으면 락 판정 신뢰도가 떨어집니다"
fi

echo
if [ "$PROBLEM" -eq 0 ]; then
  echo "  ✓ 결과 파일 이상 없음"
else
  echo "  ✗ 문제가 있습니다. 이 실행은 폐기 후보입니다(계획서 §5.1)."
fi

echo
echo "=========================================="
echo " $LABEL 완료"
echo "=========================================="
echo "  k6 시작(UTC) : $K6_START_UTC"
echo "  측정 창       : +2분 ~ +12분"
echo "  결과 파일     : $(basename "$LOCKS_CSV"), $(basename "$HIKARI_CSV")"
echo "  k6 콘솔 로그  : $(basename "$K6_LOG")"
echo "  실행 로그     : $(basename "$RUN_LOG")"
echo
echo "다음 단계:"
echo "  1. Grafana에서 이 구간의 노드 CPU·게이트웨이 메모리를 확인한다"
echo "  2. 계획서 §10.3의 수동 중단 기준에 걸린 게 없는지 본다"
echo "  3. 5분 쉰 뒤 다음 조건을 실행한다 (순서: A B B A A B)"
echo

exit "$PROBLEM"
