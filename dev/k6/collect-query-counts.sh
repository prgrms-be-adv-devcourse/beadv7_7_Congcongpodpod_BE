#!/usr/bin/env bash
#
# 스윕이 남긴 완료 로그에서 requestId·pathPattern·queryCount를 뽑아 표로 만든다.
# **ssh lastdish 서버에서 실행한다** — kubectl이 필요하다.
#
#   ./collect-query-counts.sh qcs-20260828a 30m
#
# 산출물 (현재 디렉터리):
#   <RUN_TAG>.ndjson    원본 로그 줄(우리 태그 것만)
#   <RUN_TAG>-rows.tsv  requestId별 한 줄. 쿼리 수 내림차순
#   <RUN_TAG>-by-api.tsv  API별 집계(건수·평균·최대)
#
# 이 표에는 구간 이름(step)이 없다. k6가 남긴 <RUN_TAG>-index.psv와 requestId로
# 조인해야 완성된다 — 같은 API를 size만 바꿔 두 번 부른 것을 가르는 유일한 수단이다.
# 조인 방법은 이 파일 맨 아래 주석에 있다.

set -euo pipefail

run_tag="${1:?RUN_TAG가 필요합니다. 예: ./collect-query-counts.sh qcs-20260828a}"
since="${2:-30m}"
ns="${NAMESPACE:-app}"
out="$run_tag"

# 레플리카가 여럿이면 파드마다 로그가 따로 있다.
# deploy/... 형태는 파드 하나만 읽으므로 라벨 셀렉터를 쓴다.
: > "${out}.ndjson"
for svc in core-service member-service ai-service; do
  kubectl -n "$ns" logs -l "app=$svc" --tail=-1 --since="$since" --prefix=false >> "${out}.ndjson" 2>/dev/null || {
    echo "  ⚠ $svc 로그를 읽지 못했습니다(라벨이 app=$svc가 아닐 수 있음)" >&2
  }
done

total_lines=$(wc -l < "${out}.ndjson" | tr -d ' ')
echo "수집한 로그 줄: $total_lines"

# queryCount가 있고 우리 실행 태그로 시작하는 줄만 남긴다.
# method/pathPattern/status/durationMs는 필드가 아니라 message 문자열 안에 있어 파싱한다.
extract() {
  if command -v jq >/dev/null 2>&1; then
    # -R + fromjson? : JSON이 아닌 줄(기동 로그 등)이 섞여도 멈추지 않고 건너뛴다.
    jq -rR --arg tag "$run_tag" '
      fromjson?
      | select(type == "object")
      | select(.queryCount != null)
      | select((.requestId // "") | startswith($tag))
      | (.message | capture("method=(?<method>[A-Z]+), pathPattern=(?<path>[^,]+), status=(?<status>[0-9]+), durationMs=(?<ms>[0-9]+)")) as $m
      | [ (.service.name // "-"), $m.method, $m.path, $m.status, (.queryCount|tonumber), $m.ms, .requestId ]
      | @tsv
    ' "${out}.ndjson"
  else
    # jq가 없는 서버를 위한 대체 경로. 결과 형식은 위와 같다.
    python3 - "$run_tag" "${out}.ndjson" <<'PYEOF'
import json, re, sys
tag, path = sys.argv[1], sys.argv[2]
pat = re.compile(r"method=([A-Z]+), pathPattern=([^,]+), status=(\d+), durationMs=(\d+)")
with open(path) as f:
    for line in f:
        line = line.strip()
        if not line or not line.startswith('{'):
            continue
        try:
            d = json.loads(line)
        except ValueError:
            continue
        if d.get('queryCount') is None:
            continue
        rid = d.get('requestId') or ''
        if not rid.startswith(tag):
            continue
        m = pat.search(d.get('message') or '')
        if not m:
            continue
        svc = (d.get('service') or {}).get('name', '-')
        print('\t'.join([svc, m.group(1), m.group(2), m.group(3),
                         str(int(d['queryCount'])), m.group(4), rid]))
PYEOF
  fi
}

extract | sort -t"$(printf '\t')" -k5,5nr > "${out}-rows.tsv"

rows=$(wc -l < "${out}-rows.tsv" | tr -d ' ')
echo "우리 태그의 queryCount 행: $rows"
if [ "$rows" -eq 0 ]; then
  echo "  ✗ 한 줄도 못 뽑았습니다. 확인할 것:" >&2
  echo "    - 계측이 켜져 있나 (REQUEST_LOG_COUNT_SQL_STATEMENTS=true + 롤아웃)" >&2
  echo "    - RUN_TAG가 맞나 / --since=$since 안에 스윕이 있었나" >&2
  echo "    - 파드 라벨이 app=<서비스명>이 맞나" >&2
  exit 1
fi

# API별 집계 — 같은 API를 여러 번 불렀으면 건수·평균·최대를 함께 본다.
awk -F'\t' '
  { k = $1 "\t" $2 "\t" $3; n[k]++; s[k]+=$5; if ($5 > mx[k]) mx[k]=$5 }
  END { for (k in n) printf "%s\t%d\t%.1f\t%d\n", k, n[k], s[k]/n[k], mx[k] }
' "${out}-rows.tsv" | sort -t"$(printf '\t')" -k6,6nr > "${out}-by-api.tsv"

echo
printf 'service\tmethod\tpathPattern\t건수\t평균쿼리\t최대쿼리\n'
cat "${out}-by-api.tsv"

cat <<EOF

──────────────────────────────────────────────
다음: 구간 이름(step)을 붙이려면 맥에서 조인한다.

  # 1) 서버에서 rows 파일을 맥으로 가져온다
  scp lastdish:~/${out}-rows.tsv dev/k6/results/

  # 2) k6가 남긴 index와 requestId로 조인한다
  cd dev/k6/results
  join -t'|' -1 7 -2 1 \\
    <(awk -F'\t' '{print \$1"|"\$2"|"\$3"|"\$4"|"\$5"|"\$6"|"\$7}' ${out}-rows.tsv | sort -t'|' -k7,7) \\
    <(sort -t'|' -k1,1 ${run_tag}-index.psv) \\
    > ${out}-joined.psv

조인해야 같은 API를 size만 바꿔 두 번 부른 것(sweep_scale_small/large)을
가를 수 있다 — 두 호출의 pathPattern이 똑같기 때문이다.
──────────────────────────────────────────────
EOF
