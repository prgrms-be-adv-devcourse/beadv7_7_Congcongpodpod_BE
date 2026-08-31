#!/usr/bin/env bash
#
# 실행 전후의 JVM 상태를 한 줄로 남긴다.
#
# 왜: 390을 같은 JVM에서 연속으로 돌려 "오래 뜬 JVM일수록 느려지는가"를 본다.
# 응답 시간만 재면 반복 측정일 뿐이고, GC와 힙을 함께 찍어야 원인을 말할 수 있다.
#
# process_start_time을 매번 같이 남기는 이유: 도중에 파드가 재기동하면 연속 실행이
# 아니게 된다. 값이 바뀌면 그 지점에서 계열이 끊긴 것이므로 이어 붙이면 안 된다.
#
# 사용법:  ./snapshot-jvm.sh <라벨>      예: ./snapshot-jvm.sh before-390c

set -euo pipefail

LABEL="${1:-}"
[ -z "$LABEL" ] && { echo "라벨을 지정하세요. 예: $0 before-390c" >&2; exit 2; }

ACTUATOR_URL="${ACTUATOR_URL:-http://localhost:8081/actuator/prometheus}"
OUT="${OUT:-results/jvm-snapshots.tsv}"

P=$(curl -s --max-time 10 "$ACTUATOR_URL") || { echo "actuator 응답 없음: $ACTUATOR_URL" >&2; exit 1; }
[ -z "$P" ] && { echo "actuator 응답이 비었습니다" >&2; exit 1; }

# awk에서 exit로 조기 종료하면 파이프가 먼저 닫혀 SIGPIPE가 난다(pipefail이 잡는다).
# 끝까지 읽되 첫 일치만 남긴다.
#
# v+0으로 수치화하면 안 된다 — awk 기본 OFMT가 유효숫자 6자리라 epoch(1788006266)이
# 1788010000으로 뭉개진다. 그러면 재기동을 감지하지 못한다. 원문 그대로 넘기고
# 형식은 쓰는 쪽에서 %.0f로 정한다.
val() { echo "$P" | awk -v pat="$1" '$0 ~ pat && !seen {v=$NF; seen=1} END {print v}'; }
sum() { echo "$P" | awk -v pat="$1" '$0 ~ pat {s+=$NF} END {printf "%.3f", s+0}'; }

start=$(val '^process_start_time_seconds')
uptime=$(val '^process_uptime_seconds')
minor_n=$(sum '^jvm_gc_pause_seconds_count.*minor GC')
minor_s=$(sum '^jvm_gc_pause_seconds_sum.*minor GC')
major_n=$(sum '^jvm_gc_pause_seconds_count.*major GC')
major_s=$(sum '^jvm_gc_pause_seconds_sum.*major GC')
tenured=$(val 'jvm_memory_used_bytes.*Tenured Gen')
eden=$(val 'jvm_memory_used_bytes.*Eden Space')
threads=$(val '^jvm_threads_live_threads')
peak=$(val '^jvm_threads_peak_threads')
hik_act=$(val '^hikaricp_connections_active')
hik_pend=$(val '^hikaricp_connections_pending')

mkdir -p "$(dirname "$OUT")"
if [ ! -f "$OUT" ]; then
  printf 'label\tsampled_utc\tprocess_start\tuptime_s\tminor_gc_n\tminor_gc_s\tmajor_gc_n\tmajor_gc_s\ttenured_mb\teden_mb\tthreads\tpeak_threads\thikari_active\thikari_pending\n' > "$OUT"
fi

# 과학적 표기(9.2E7)로 오므로 awk에 맡긴다. 빈 값은 0으로 본다.
mb() { awk -v x="${1:-0}" 'BEGIN {printf "%.1f", (x+0)/1048576}'; }

num() { awk -v x="${1:-0}" 'BEGIN {printf "%.0f", x+0}'; }

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "$LABEL" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$(num "$start")" "$(num "$uptime")" \
  "$minor_n" "$minor_s" "$major_n" "$major_s" \
  "$(mb "$tenured")" "$(mb "$eden")" "$threads" "$peak" "$hik_act" "$hik_pend" >> "$OUT"

echo "기록: $LABEL  (uptime $(num "$uptime")s, minor GC ${minor_n}회/${minor_s}s, major GC ${major_n}회/${major_s}s, Tenured $(mb "$tenured")MB)"
