#!/usr/bin/env bash
#
# PR #463(매장 목록 조회의 휴무일 N+1 제거) 배포 후 효과를 확인한다.
#
# 요청당 쿼리 수는 응답이 아니라 서버 로그에만 남는다. 이 스크립트는 프로브를 보내고
# **반환 건수와 서로 다른 매장 수를 기록**한 뒤, 로그에서 읽을 명령을 찍는다.
# 쿼리 수와 매장 수를 짝지어야 "매장당 1쿼리"가 사라졌는지 판정할 수 있다.
#
# 전제: 운영에 계측이 켜져 있어야 한다(REQUEST_LOG_COUNT_SQL_STATEMENTS=true).
#
#   ./after-463-check.sh              # 기본 태그 after463
#   ./after-463-check.sh mytag        # 태그 직접 지정
#
# 판정 (before는 2026-08-28 qcs-20260828a):
#   /orders    3 + 서로 다른 매장 수  ->  크기와 무관하게 3
#   /favorites 1 + 1 + N + 1         ->  찜 개수와 무관하게 4

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
tag="${1:-after463}"

if [[ ! -f "$script_dir/.env" ]]; then
  echo "dev/k6/.env가 없습니다." >&2
  exit 2
fi
set -a
# shellcheck disable=SC1091
source "$script_dir/.env"
set +a

api="${BASE_URL%/}/api/v1"
account="${AFTER_ACCOUNT:-seller0001@seed.lastdish.kr}"

body_file="$(mktemp "${TMPDIR:-/tmp}/after463.XXXXXX")"
trap 'rm -f "$body_file"' EXIT

# 응답 본문은 파일로 넘긴다. 한글 매장명을 파이썬 소스에 직접 박으면 인코딩 오류가 난다.
readonly PARSE_PAGE='
import json, os, sys
rid, label = os.environ["P_ID"], os.environ["P_LABEL"]
try:
    with open(sys.argv[1], encoding="utf-8") as f:
        data = json.load(f)["data"]
    rows = data.get("content") or []
    stores = len({row["storeId"] for row in rows})
    print("  %-24s %-20s rows=%2d  stores=%2d" % (rid, label, len(rows), stores))
except Exception as exc:
    print("  %-24s %-20s parse failed: %s" % (rid, label, exc))
'

readonly PARSE_FAV='
import json, os, sys
rid = os.environ["P_ID"]
try:
    with open(sys.argv[1], encoding="utf-8") as f:
        rows = json.load(f).get("data") or []
    print("  %-24s %-20s count=%d" % (rid, "favorites", len(rows)))
    if not rows:
        print("  (!) 찜한 매장이 없어 이 값만으로는 N+1 해소를 판정할 수 없다.")
        print("      필요하면 POST /favorites로 몇 개 추가해 재고, 끝나면 DELETE로 원복한다.")
except Exception as exc:
    print("  %-24s parse failed: %s" % (rid, exc))
'

echo "=========================================="
echo " PR #463 after 측정 — 태그 $tag"
echo "=========================================="
echo "  대상 : $api"
echo "  계정 : $account"
echo

token="$(curl -fsS --max-time 15 -X POST "$api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$account\",\"password\":\"$SEED_PASSWORD\"}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')"

if [[ -z "$token" ]]; then
  echo "✗ 로그인 실패" >&2
  exit 1
fi
echo "  ✓ 로그인"
echo

probe_orders() {
  local id="$1" query="$2" label="$3"
  curl -fsS --max-time 20 "$api/orders?$query" \
    -H "Authorization: Bearer $token" -H "X-Request-Id: $id" > "$body_file" 2>/dev/null || true
  P_ID="$id" P_LABEL="$label" python3 -c "$PARSE_PAGE" "$body_file"
}

echo "[주문 목록] 크기와 상태를 바꿔 매장 수를 흔든다 — 쿼리는 고정이어야 한다"
probe_orders "$tag-ord-a" "page=0&size=5"                    "size=5"
probe_orders "$tag-ord-b" "page=0&size=20"                   "size=20"
probe_orders "$tag-ord-c" "page=0&size=50"                   "size=50"
probe_orders "$tag-ord-d" "page=0&size=50&status=PICKED_UP"  "size=50 PICKED_UP"
probe_orders "$tag-ord-e" "page=0&size=50&status=RESERVED"   "size=50 RESERVED"

echo
echo "[즐겨찾기]"
curl -fsS --max-time 20 "$api/favorites" \
  -H "Authorization: Bearer $token" -H "X-Request-Id: $tag-fav-a" > "$body_file" 2>/dev/null || true
P_ID="$tag-fav-a" python3 -c "$PARSE_FAV" "$body_file"

cat <<EOF

──────────────────────────────────────────────
다음: 서버에서 쿼리 수를 읽는다.

  kubectl -n app logs -l app=core-service --tail=400 --prefix=false \\
    | grep '$tag-' \\
    | grep -oE '"requestId":"[^"]*"|"queryCount":"[0-9]*"' \\
    | paste - -

판정
  /orders     크기·매장 수와 무관하게 3이면 성공
              (before: 3 + 서로 다른 매장 수 → size=50에서 34)
  /favorites  찜 개수와 무관하게 4면 성공
              (before: 1 + 1 + 찜한 매장 수 + 1)

배포 확인 (태그가 :dev라 다이제스트로만 세대를 알 수 있다)
  kubectl -n app describe pod -l app=core-service | grep "Image ID:"
  before: sha256:3b686a7ab665292be5390203141326267397c3e8d52083bfdd3579eead6d9796
  → 이 값과 달라야 새 코드다.
──────────────────────────────────────────────
EOF
