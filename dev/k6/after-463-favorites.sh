#!/usr/bin/env bash
#
# PR #463의 효과를 즐겨찾기 경로에서도 확인한다.
#
# /orders와 같은 findActiveStores를 쓰므로 같은 N+1이 있었다. 다만 즐겨찾기는
# holidays를 실제로 응답에 담으므로 원래 필요한 데이터였다.
#
# **운영 데이터를 잠깐 바꾼다.** 찜을 추가해 재고, 끝나면 반드시 원복한다.
# 어느 경로로 빠져나가든 trap이 삭제를 보장한다.
#
#   ./after-463-favorites.sh [태그]
#
# 판정: 찜 개수가 2곳이든 5곳이든 queryCount가 같으면(4) N+1이 없는 것이다.
#   before 공식: 1(찜 목록) + 1(매장 일괄) + 찜한 매장 수(holidays) + 1(상품 일괄)

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
tag="${1:-after463fav}"

set -a
# shellcheck disable=SC1091
source "$script_dir/.env"
set +a

api="${BASE_URL%/}/api/v1"
account="${AFTER_ACCOUNT:-seller0001@seed.lastdish.kr}"

# 시드에 실재하고 OPEN인 매장들. 집중도 실험 대상 풀과 겹치지 않게 뒤쪽을 쓴다.
STORES=(501 502 503 504 505)
added=()

token="$(curl -fsS --max-time 15 -X POST "$api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$account\",\"password\":\"$SEED_PASSWORD\"}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')"

# 어떤 경로로 끝나든 추가한 찜을 되돌린다.
cleanup() {
  local rc=$?
  if [[ ${#added[@]} -gt 0 ]]; then
    echo
    echo "[원복] 추가한 찜 ${#added[@]}건 삭제"
    for sid in "${added[@]}"; do
      local code
      code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 -X DELETE \
        "$api/favorites/$sid" -H "Authorization: Bearer $token" || echo 000)"
      echo "  store $sid 삭제 → $code"
    done
    local left
    left="$(curl -fsS --max-time 15 "$api/favorites" -H "Authorization: Bearer $token" \
      | python3 -c 'import sys,json;print(len(json.load(sys.stdin).get("data") or []))' 2>/dev/null || echo '?')"
    echo "  남은 찜: ${left}곳 (0이어야 정상)"
  fi
  exit $rc
}
trap cleanup EXIT

echo "=========================================="
echo " 즐겨찾기 N+1 확인 — 태그 $tag"
echo "=========================================="

before_count="$(curl -fsS --max-time 15 "$api/favorites" -H "Authorization: Bearer $token" \
  | python3 -c 'import sys,json;print(len(json.load(sys.stdin).get("data") or []))')"
echo "  시작 시점 찜: ${before_count}곳"
if [[ "$before_count" != "0" ]]; then
  echo "  ✗ 찜이 비어 있지 않습니다. 원복이 복잡해지므로 중단합니다." >&2
  exit 2
fi
echo

add_favorite() {
  local sid="$1" code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 -X POST "$api/favorites" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"storeId\":$sid}")"
  if [[ "$code" == "200" || "$code" == "201" ]]; then
    added+=("$sid")
    echo "  store $sid 추가 → $code"
  else
    echo "  store $sid 추가 실패 → $code" >&2
  fi
}

measure() {
  local id="$1" expect="$2" body_file
  body_file="$(mktemp "${TMPDIR:-/tmp}/fav.XXXXXX")"
  curl -fsS --max-time 20 "$api/favorites" \
    -H "Authorization: Bearer $token" -H "X-Request-Id: $id" > "$body_file"
  P_ID="$id" P_EXPECT="$expect" python3 -c '
import json, os, sys
with open(sys.argv[1], encoding="utf-8") as f:
    rows = json.load(f).get("data") or []
print("  %-26s 찜 %s곳 반환 (기대 %s)" % (os.environ["P_ID"], len(rows), os.environ["P_EXPECT"]))
' "$body_file"
  rm -f "$body_file"
}

echo "[1단계] 찜 2곳"
add_favorite "${STORES[0]}"
add_favorite "${STORES[1]}"
measure "$tag-2" 2

echo
echo "[2단계] 찜 5곳"
add_favorite "${STORES[2]}"
add_favorite "${STORES[3]}"
add_favorite "${STORES[4]}"
measure "$tag-5" 5

cat <<EOF

──────────────────────────────────────────────
서버에서 쿼리 수를 읽는다.

  kubectl -n app logs -l app=core-service --tail=200 --prefix=false \\
    | grep '$tag-' | grep queryCount \\
    | python3 -c '
import sys, json
for line in sys.stdin:
    line = line.strip()
    if not line.startswith("{"):
        continue
    try:
        d = json.loads(line)
    except ValueError:
        continue
    print("  %-26s queryCount=%s" % (d.get("requestId"), d.get("queryCount")))
'

판정
  $tag-2 (찜 2곳)  before 공식이면 5,  수정됐으면 4
  $tag-5 (찜 5곳)  before 공식이면 8,  수정됐으면 4
  → 둘이 같으면 N+1이 사라진 것이다.
──────────────────────────────────────────────
EOF
