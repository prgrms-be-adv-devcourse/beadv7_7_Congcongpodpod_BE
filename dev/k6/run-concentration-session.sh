#!/usr/bin/env bash
#
# 집중도 A/B 세션 전체(A B B A A B = uniform 1·weighted 1·weighted 2·uniform 2·uniform 3·weighted 3)를
# 회차 사이 5분 휴식을 두고 자동으로 이어 돌린다.
#
# run-concentration-ab.sh는 원래 한 번만 돌리고 "다음 실행 여부는 사람이 판단"하도록 의도적으로
# 자동 연속 실행을 안 만들었다(계획서 §10 — 실행 중 자리를 비우지 않는다). 이 스크립트는 그 판단 중
# **자동으로 검사 가능한 §10.3 기준만** 회차마다 확인해서 걸리면 즉시 멈추는 조건으로 자동화한다.
#
#   - 주문 생성 성공률 95% 미만          → k6 요약(flow_order_create_success)에서 확인
#   - dropped_iterations 5% 초과         → k6 요약(dropped_iterations)에서 확인
#   - 5xx·네트워크 실패 3% 이상          → k6 요약(flow_infrastructure_failures). capacity-ladder.js
#                                          자체 threshold(abortOnFail)로 k6 실행 중에도 이미 걸린다
#   - order_create p95가 기준선(첫 uniform) 3배 초과 → uniform에서는 단독으로 중단,
#                                          weighted에서는 계획서 §8 예외대로 다른 기준과 같이 걸릴 때만 중단
#   - HikariCP pending>0이 60초 이상 연속 → 이 회차의 hikari CSV를 직접 훑어서 확인
#
# **자동으로 못 보는 기준(계획서 §10.3에 있지만 이 스크립트가 검사하지 않음)**:
#   게이트웨이 메모리 95% 이상, Pod restart/OOMKilled, Prometheus target down.
#   이건 로컬에서 kubectl·게이트웨이 actuator에 접근할 방법이 없어서다. 세션이 도는 동안
#   Grafana를 계속 열어두고 이 세 가지는 사람이 직접 봐야 한다.
#
# 사용법:
#   ARCHIVE_EXISTING=1 ./run-concentration-session.sh
#   REST_SECONDS=60 ./run-concentration-session.sh   # 휴식 시간을 바꾸고 싶을 때(기본 300초)

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
results_dir="$script_dir/results"
cd "$script_dir"

if [[ ! -f .env ]]; then
  echo "dev/k6/.env가 없습니다." >&2
  exit 2
fi
set -a
# shellcheck disable=SC1091
source .env
set +a

REST_SECONDS="${REST_SECONDS:-300}"
DEFAULT_SEQUENCE=(uniform:1 weighted:1 weighted:2 uniform:2 uniform:3 weighted:3)
# 이미 끝낸 회차가 있으면 SEQUENCE="weighted:1 weighted:2 ..." 형태로 넘겨서 이어서 돌린다.
if [[ -n "${SEQUENCE:-}" ]]; then
  # shellcheck disable=SC2206
  SEQUENCE=($SEQUENCE)
else
  SEQUENCE=("${DEFAULT_SEQUENCE[@]}")
fi

BASELINE_P95=""
# 이어서 돌리는 경우, 이미 끝난 첫 uniform 회차의 p95를 기준선으로 되살린다.
first_uniform_summary="$results_dir/${RUN_ID}-r2-conc-uniform-1-summary.json"
if [[ -f "$first_uniform_summary" ]]; then
  BASELINE_P95="$(python3 -c "
import json
d = json.load(open('$first_uniform_summary'))
p95 = d.get('metrics', {}).get('step_order_create', {}).get('values', {}).get('p(95)')
print(p95 if p95 is not None else '')
" 2>/dev/null || true)"
  if [[ -n "$BASELINE_P95" ]]; then
    echo "  기준선 p95 복원(기존 uniform 1 결과): ${BASELINE_P95}ms"
  fi
fi

# 한 회차의 k6 요약 JSON을 검사한다. 문제가 있으면 STOP: 줄을 찍고 1로 끝난다.
# 문제가 없으면(첫 uniform이면) P95=<ms> 줄을 찍고 0으로 끝난다.
check_summary() {
  local dist="$1" summary="$2" baseline="$3"
  python3 - "$dist" "$summary" "$baseline" <<'PYEOF'
import json
import sys

dist, summary_path, baseline = sys.argv[1], sys.argv[2], sys.argv[3]

try:
    d = json.load(open(summary_path))
except FileNotFoundError:
    print(f"STOP: 요약 파일이 없습니다: {summary_path}")
    sys.exit(1)

m = d.get("metrics", {})


def rate(name, default=1.0):
    return m.get(name, {}).get("values", {}).get("rate", default)


def count(name, default=0):
    return m.get(name, {}).get("values", {}).get("count", default)


order_success = rate("flow_order_create_success", 1.0)
iterations = count("iterations", 0)
dropped = count("dropped_iterations", 0)
infra_rate = rate("flow_infrastructure_failures", 0.0)
p95 = m.get("step_order_create", {}).get("values", {}).get("p(95)")

problems = []
if order_success < 0.95:
    problems.append(f"주문 생성 성공률 {order_success:.1%} < 95%")

attempted = iterations + dropped
drop_rate = (dropped / attempted) if attempted else 0.0
if drop_rate > 0.05:
    problems.append(f"dropped_iterations {drop_rate:.1%} > 5%")

if infra_rate >= 0.03:
    problems.append(f"5xx·네트워크 실패 {infra_rate:.1%} >= 3%")

if baseline and p95 is not None:
    baseline_p95 = float(baseline)
    if p95 > baseline_p95 * 3:
        if dist == "weighted":
            print(
                f"NOTE: order_create p95 {p95:.0f}ms가 기준선 3배({baseline_p95 * 3:.0f}ms) "
                "초과 — weighted는 이 항목 단독으로는 중단하지 않는다(계획서 §8 예외)",
                file=sys.stderr,
            )
        else:
            problems.append(
                f"order_create p95 {p95:.0f}ms > 기준선 3배({baseline_p95 * 3:.0f}ms)"
            )

if problems:
    for p in problems:
        print(f"STOP: {p}")
    sys.exit(1)

if p95 is not None:
    print(f"P95={p95}")
sys.exit(0)
PYEOF
}

# hikari CSV에서 pending>0이 60행(≈60초, 1초 간격 샘플) 이상 연속됐는지 본다.
check_hikari_pending() {
  local hikari_csv="$1"
  python3 - "$hikari_csv" <<'PYEOF'
import csv
import sys

path = sys.argv[1]
streak = 0
max_streak = 0
try:
    with open(path) as f:
        reader = csv.DictReader(f, delimiter="|")
        for row in reader:
            raw = row.get("pending", "")
            try:
                pending = float(raw)
            except (TypeError, ValueError):
                streak = 0
                continue
            if pending > 0:
                streak += 1
                max_streak = max(max_streak, streak)
            else:
                streak = 0
except FileNotFoundError:
    sys.exit(0)

if max_streak >= 60:
    print(f"STOP: HikariCP pending>0이 {max_streak}초 연속 지속(60초 기준)")
    sys.exit(1)
sys.exit(0)
PYEOF
}

echo "=========================================="
echo " 집중도 A/B 세션 자동 실행 — A B B A A B"
echo "=========================================="
echo "  순서: ${SEQUENCE[*]}"
echo "  회차 간 휴식: ${REST_SECONDS}초"
echo "  ⚠ 게이트웨이 메모리·Pod 재시작·OOMKilled는 이 스크립트가 못 봅니다."
echo "    Grafana를 계속 열어두고 직접 지켜봐 주세요."
echo

first=true
for entry in "${SEQUENCE[@]}"; do
  dist="${entry%%:*}"
  no="${entry##*:}"

  if [[ "$first" == false ]]; then
    echo "[휴식] ${REST_SECONDS}초 대기 중..."
    sleep "$REST_SECONDS"
    echo
  fi
  first=false

  echo "=========================================="
  echo " 회차 시작: $dist $no"
  echo "=========================================="

  if ! ARCHIVE_EXISTING=1 "$script_dir/run-concentration-ab.sh" "$dist" "$no"; then
    echo "✗ run-concentration-ab.sh 자체가 문제(결과 파일 이상 등)로 끝났습니다: $dist $no" >&2
    echo "  세션을 여기서 중단합니다. 원인을 확인한 뒤 이어서 수동으로 실행하세요." >&2
    exit 1
  fi

  label="r2-conc-${dist}-${no}"
  summary="$results_dir/${RUN_ID}-${label}-summary.json"
  hikari_csv="$results_dir/hikari-${label}.csv"

  echo
  echo "[자동 중단 기준 검사: $dist $no]"

  check_output="$(check_summary "$dist" "$summary" "$BASELINE_P95")" || {
    echo "$check_output"
    echo "✗ 자동 중단 기준에 걸렸습니다: $dist $no" >&2
    echo "  세션을 여기서 중단합니다." >&2
    exit 1
  }
  echo "$check_output"

  if ! check_hikari_pending "$hikari_csv"; then
    echo "✗ HikariCP pending 지속 기준에 걸렸습니다: $dist $no" >&2
    echo "  세션을 여기서 중단합니다." >&2
    exit 1
  fi

  if [[ "$dist" == uniform && -z "$BASELINE_P95" ]]; then
    p95_line="$(echo "$check_output" | grep '^P95=' || true)"
    if [[ -n "$p95_line" ]]; then
      BASELINE_P95="${p95_line#P95=}"
      echo "  기준선 p95 설정(첫 uniform): ${BASELINE_P95}ms"
    fi
  fi

  echo "✓ $dist $no 통과 — 다음으로 진행"
  echo
done

echo "=========================================="
echo " 세션 전체(A B B A A B) 완료"
echo "=========================================="
