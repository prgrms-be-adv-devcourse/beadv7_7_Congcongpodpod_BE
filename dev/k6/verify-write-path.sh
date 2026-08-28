#!/usr/bin/env bash
#
# PR #472(주문 생성 24 -> 16쿼리) 배포 후 효과를 확인한다.
#
# 요청당 쿼리 수는 응답이 아니라 서버 로그에만 남으므로 계측이 켜져 있어야 한다.
# 평시에는 꺼 두는 것이 원칙이라(#422) 켜고 끄는 것은 사람이 하고, 이 스크립트는
# 프로브만 보낸 뒤 로그에서 읽을 명령을 찍는다.
#
#   ./verify-write-path.sh [태그]
#
# **운영 데이터를 바꾼다.** 시드 계정으로 주문 1건을 만들고 픽업까지 끝내 정리한다.
# 재고 1개가 줄고 예치금이 차감되며 그대로 남는다 — 부하 테스트가 늘 하던 것과 같다.
#
# 판정 (before는 2026-08-28 실측)
#   order_create  24 -> 16   (이번 수정의 주인공)
#   order_accept   9 -> ?    Outbox 수정은 공용 모듈이라 이 경로도 같이 준다
#   order_pickup  10 -> ?
#   cart_add       4 -> ?

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
tag="${1:-after472}"

[[ -f "$script_dir/.env" ]] || { echo "dev/k6/.env가 없습니다." >&2; exit 2; }
set -a
# shellcheck disable=SC1091
source "$script_dir/.env"
set +a

api="${BASE_URL%/}/api/v1"
buyer="${VERIFY_BUYER:-seller0001@seed.lastdish.kr}"
seller="${VERIFY_SELLER:-seller0501@seed.lastdish.kr}"

body="$(mktemp "${TMPDIR:-/tmp}/vwp.XXXXXX")"
trap 'rm -f "$body"' EXIT

login() {
  curl -fsS --max-time 15 -X POST "$api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$SEED_PASSWORD\"}" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])'
}

# 응답 본문을 파일로 넘긴다. 한글 매장명을 파이썬 소스에 박으면 인코딩 오류가 난다.
pick() { P_EXPR="$1" python3 -c '
import json, os, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))["data"]
print(eval("data" + os.environ["P_EXPR"]))
' "$body"; }

echo "=========================================="
echo " PR #472 쓰기 경로 검증 — 태그 $tag"
echo "=========================================="
echo "  대상  : $api"
echo "  구매자: $buyer"
echo "  판매자: $seller"
echo
echo "  ⚠️  계측이 켜져 있어야 합니다."
echo "      kubectl -n app set env deploy/core-service REQUEST_LOG_COUNT_SQL_STATEMENTS=true"
echo "      kubectl -n app rollout status deploy/core-service --timeout=600s"
read -r -p "  켜져 있으면 Enter, 중단은 Ctrl-C: " _
echo

buyer_token="$(login "$buyer")"
seller_token="$(login "$seller")"
echo "  ✓ 로그인"

# 판매자 매장의 상품을 찾는다.
curl -fsS --max-time 15 "$api/stores/mine" -H "Authorization: Bearer $seller_token" > "$body"
store_id="$(pick "[0]['storeId']")"
curl -fsS --max-time 15 "$api/stores/$store_id/dish" -H "Authorization: Bearer $seller_token" > "$body"
dish_id="$(pick "['dishId']")"
echo "  매장 $store_id · 상품 $dish_id"

# 구매자 장바구니를 가져온다. 비어 있지 않으면 이전 실행의 잔여이므로 중단한다.
curl -fsS --max-time 15 "$api/carts/members" -H "Authorization: Bearer $buyer_token" > "$body"
cart_id="$(pick "['cartId']")"
existing="$(pick "len(data.get('items') or [])")"
if [[ "$existing" != "0" ]]; then
  echo "  ✗ 장바구니에 ${existing}건이 남아 있습니다. 비우고 다시 실행하세요." >&2
  exit 2
fi
echo "  장바구니 $cart_id (비어 있음)"
echo

echo "[1/4] 장바구니 담기"
curl -fsS --max-time 20 -X POST "$api/carts/$cart_id/items" \
  -H "Authorization: Bearer $buyer_token" -H 'Content-Type: application/json' \
  -H "X-Request-Id: $tag-cart-add" \
  -d "{\"dishId\":$dish_id,\"quantity\":1}" > "$body"
cart_item_id="$(pick "['cartItemId']")"
price_version="$(pick "data.get('lastAppliedDishPriceVersion') or 0")"
echo "      항목 $cart_item_id · priceVersion $price_version"

echo "[2/4] 주문 생성  ← 이번 수정의 주인공"
curl -fsS --max-time 20 -X POST "$api/orders/cartItems/$cart_item_id" \
  -H "Authorization: Bearer $buyer_token" -H 'Content-Type: application/json' \
  -H "X-Request-Id: $tag-order-create" \
  -d "{\"dishPriceVersion\":$price_version}" > "$body"
order_id="$(pick "['orderId']")"
echo "      주문 $order_id"

echo "[3/4] 주문 수락"
curl -fsS --max-time 20 -X POST "$api/orders/$order_id/accept" \
  -H "Authorization: Bearer $seller_token" -H "X-Request-Id: $tag-order-accept" > /dev/null

echo "[4/4] 픽업 완료 (주문 정리)"
curl -fsS --max-time 20 -X PATCH "$api/orders/$order_id/pickup" \
  -H "Authorization: Bearer $seller_token" -H 'Content-Type: application/json' \
  -H "X-Request-Id: $tag-order-pickup" -d '{"status":"PICKED_UP"}' > /dev/null

cat <<REPORT

──────────────────────────────────────────────
서버에서 쿼리 수를 읽는다.

  kubectl -n app logs -l app=core-service --tail=600 --prefix=false \\
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
    print("  %-24s queryCount=%s" % (d.get("requestId"), d.get("queryCount")))
'

판정 (before는 2026-08-28 실측)
  $tag-order-create   24 -> **16**이면 성공
  $tag-order-accept    9 -> Outbox 이벤트가 붙는 만큼 감소 기대
  $tag-order-pickup   10 -> 같음
  $tag-cart-add        4 -> 이벤트가 없으면 그대로 4

측정이 끝나면 계측을 반드시 끕니다.
  kubectl -n app set env deploy/core-service REQUEST_LOG_COUNT_SQL_STATEMENTS-
  kubectl -n app rollout status deploy/core-service --timeout=600s
──────────────────────────────────────────────
REPORT
