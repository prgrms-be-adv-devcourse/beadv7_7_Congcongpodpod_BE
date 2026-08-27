#!/usr/bin/env bash
#
# 부하 실행 중 HikariCP 커넥션 풀 지표를 1초 간격으로 표본 수집한다.
#
# monitor-db-locks.sh와 짝을 이루는 스크립트다. 1라운드 집중도 A/B(weighted)에서
# 판매자 탐색(Dish 행 락을 전혀 안 잡는 읽기 전용 조회)까지 3.4배 느려진 것이,
# "인기 상품 행 락 경합" 하나만으로는 설명되지 않는다(2라운드 설계 §5.2.1). 락 대기가
# 적은데 이 스크립트의 pending이 올라가 있다면, 원인은 락이 아니라 커넥션 풀 포화다.
#
# 사용법 (weighted 실행과 동시에, monitor-db-locks.sh와 같이 별도 터미널에서):
#   TARGET_URL=http://localhost:8081/actuator/prometheus ./monitor-hikari.sh
#
# 환경변수:
#   TARGET_URL         필수. core-service의 /actuator/prometheus 주소.
#                       기본값을 두지 않는다 — 레포에 포트가 명시돼 있지 않아, 잘못
#                       추측한 기본값으로 조용히 딴 곳을 재는 것보다 실패가 낫다.
#   DURATION_SECONDS   기본 900 (monitor-db-locks.sh와 동일)
#   INTERVAL_SECONDS   기본 1   (monitor-db-locks.sh와 동일)
#   OUTPUT_FILE        기본 ./results/hikari-<타임스탬프>.csv
#
# 대조 시 주의: 이 스크립트의 sampled_at은 애플리케이션 서버 시계이고,
# monitor-db-locks.sh의 sampled_at은 DB 서버 시계다. 두 서버 시계가 NTP로 맞춰져
# 있지 않으면 초 단위 선후관계 판정이 어긋날 수 있다. 실행 전에 두 서버 시각을 한 번
# 대조해 두는 것을 권한다.
#
# hikaricp_connections_acquire_seconds_sum/_count는 애플리케이션 기동 이후 누적값이라
# (Prometheus Summary 타입), 매 폴링 시점의 값을 그대로 쓰면 안 된다. 직전 표본과의
# 차이(델타)로 "이번 1초 동안 커넥션을 얻는 데 평균 몇 ms 걸렸는지"를 계산한다.

set -euo pipefail

if [ -z "${TARGET_URL:-}" ]; then
  echo "TARGET_URL이 필요합니다. 예: TARGET_URL=http://localhost:8081/actuator/prometheus $0" >&2
  exit 2
fi

DURATION_SECONDS="${DURATION_SECONDS:-900}"
INTERVAL_SECONDS="${INTERVAL_SECONDS:-1}"

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
results_dir="$script_dir/results"
mkdir -p "$results_dir"
OUTPUT_FILE="${OUTPUT_FILE:-$results_dir/hikari-$(date +%Y%m%d-%H%M%S).csv}"

# monitor-db-locks.sh와 같은 구분자(|)와 같은 첫 컬럼 이름(sampled_at)을 쓴다.
# 헤더는 아래 한 표본을 만드는 로직의 필드 순서와 반드시 같아야 한다.
echo "sampled_at|active|pending|idle|acquire_avg_ms|acquire_count_delta" > "$OUTPUT_FILE"

echo "HikariCP 지표 수집 시작 — $TARGET_URL, ${DURATION_SECONDS}초 동안 ${INTERVAL_SECONDS}초 간격"
echo "결과 파일: $OUTPUT_FILE"

# 누적값(Summary)의 직전 표본. 첫 표본에서는 델타를 낼 기준이 없어 비워 둔다.
prev_acquire_sum=""
prev_acquire_count=""

end_at=$(($(date +%s) + DURATION_SECONDS))

while [ "$(date +%s)" -lt "$end_at" ]; do
  # 밀리초(%3N)는 GNU date 전용이라 macOS(BSD date)에서는 리터럴 "3N"이 그대로 찍힌다.
  # 어차피 폴링 간격이 1초라 밀리초 정밀도가 필요 없어 초 단위로 낮춰 두 플랫폼 모두
  # 동작하게 한다.
  sampled_at="$(date '+%Y-%m-%d %H:%M:%S')"

  body="$(curl -fsS --max-time 3 "$TARGET_URL" 2>>"$OUTPUT_FILE.err" || true)"

  if [ -z "$body" ]; then
    echo "$sampled_at|ERROR|ERROR|ERROR|ERROR|ERROR" >> "$OUTPUT_FILE"
    sleep "$INTERVAL_SECONDS"
    continue
  fi

  # 값 하나만 뽑는다. 첫 매칭 라인의 마지막 필드(공백 구분)가 값이다.
  # 데이터소스가 여러 개라 풀이 둘 이상이면 첫 번째 풀만 잡힌다 — 지금은
  # core-service 단일 풀만 본다.
  active="$(printf '%s\n' "$body" | grep '^hikaricp_connections_active' | head -1 | awk '{print $NF}')"
  pending="$(printf '%s\n' "$body" | grep '^hikaricp_connections_pending' | head -1 | awk '{print $NF}')"
  idle="$(printf '%s\n' "$body" | grep '^hikaricp_connections_idle' | head -1 | awk '{print $NF}')"
  acquire_sum="$(printf '%s\n' "$body" | grep '^hikaricp_connections_acquire_seconds_sum' | head -1 | awk '{print $NF}')"
  acquire_count="$(printf '%s\n' "$body" | grep '^hikaricp_connections_acquire_seconds_count' | head -1 | awk '{print $NF}')"

  acquire_avg_ms=""
  acquire_count_delta=""
  if [ -n "$acquire_sum" ] && [ -n "$acquire_count" ] && [ -n "$prev_acquire_sum" ]; then
    acquire_count_delta="$(awk -v c="$acquire_count" -v pc="$prev_acquire_count" 'BEGIN{printf "%.0f", c-pc}')"
    if [ "$acquire_count_delta" != "0" ]; then
      acquire_avg_ms="$(awk -v s="$acquire_sum" -v ps="$prev_acquire_sum" -v cd="$acquire_count_delta" \
        'BEGIN{printf "%.2f", (s-ps)/cd*1000}')"
    else
      acquire_avg_ms="0"
    fi
  fi

  echo "$sampled_at|${active:-}|${pending:-}|${idle:-}|${acquire_avg_ms}|${acquire_count_delta}" >> "$OUTPUT_FILE"

  prev_acquire_sum="$acquire_sum"
  prev_acquire_count="$acquire_count"

  sleep "$INTERVAL_SECONDS"
done

echo "수집 종료. $(wc -l < "$OUTPUT_FILE") 행 기록됨 (헤더 포함)."
if [ -s "$OUTPUT_FILE.err" ]; then
  echo "경고: $OUTPUT_FILE.err 에 에러 로그가 있다. 접속 실패가 조용히 반복됐을 수 있으니 확인한다."
fi
