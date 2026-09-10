#!/usr/bin/env bash
#
# PR #472(주문 생성 24 -> 16쿼리) 배포 후 효과를 확인한다.
#
# 요청당 쿼리 수는 응답이 아니라 서버 로그에만 남으므로 계측이 켜져 있어야 한다.
# 평시에는 꺼 두는 것이 원칙이라(#422) 켜고 끄는 것은 사람이 하고, 이 스크립트는
# 프로브만 보낸 뒤 로그에서 읽을 명령을 찍는다.
#
#   PREFLIGHT_ONLY=1 ./verify-write-path.sh    # 계측 켜기 전에 먼저 이것부터
#   ./verify-write-path.sh [태그]              # 실제 측정
#
# **PREFLIGHT_ONLY를 먼저 돌린다.** 계측을 켜려면 파드 롤아웃(3~10분)이 필요한데,
# 스크립트가 깨져 있으면 그 시간을 버리고 다시 롤아웃해야 한다. 드라이런은
# 로그인·계정 상태·재고·잔액만 확인하고 데이터를 바꾸지 않는다.
#
# **실측 모드는 운영 데이터를 바꾼다.** 시드 계정으로 주문 1건을 만들고 픽업까지
# 끝내 정리한다. 재고 1개와 예치금이 실제로 차감돼 남는다 — 부하 테스트가 늘 하던 것과 같다.
#
# 판정 (before는 2026-08-28 실측)
#   order_create  24 -> 16   (이번 수정의 주인공)
#   order_accept   9 -> ?    Outbox 수정은 공용 모듈이라 이 경로도 같이 준다
#   order_pickup  10 -> ?
#   cart_add       4 -> ?

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
tag="${1:-after472}"
preflight_only="${PREFLIGHT_ONLY:-}"

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

fail() { echo "  ✗ $1" >&2; exit 1; }

login() {
  curl -fsS --max-time 15 -X POST "$api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$SEED_PASSWORD\"}" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])' \
    || fail "로그인 실패: $1"
}

# 응답 본문은 파일로 넘긴다. 한글 매장명을 파이썬 소스에 박으면 인코딩 오류가 난다.
#
# 인자는 파이썬 식 하나를 통째로 받는다. 앞 버전은 "data" 문자열에 인자를 이어 붙였는데,
# 공백을 빠뜨려 dataand len(data)처럼 깨졌다(2026-08-29 드라이런에서 발견).
#
# data는 응답에서 꺼낸 알맹이다. 대부분 API가 ApiResponse로 감싸 {"data": ...}로 주지만
# GET /deposits/balance는 감싸지 않고 {"memberId":..,"balance":..}를 그대로 준다.
# 둘 다 받도록 여기서 흡수한다.
pick() { P_EXPR="$1" python3 -c '
import json, os, sys
raw = json.load(open(sys.argv[1], encoding="utf-8"))
data = raw["data"] if isinstance(raw, dict) and "data" in raw else raw
print(eval(os.environ["P_EXPR"]))
' "$body"; }

echo "=========================================="
if [[ -n "$preflight_only" ]]; then
  echo " 드라이런 — 데이터를 바꾸지 않습니다"
else
  echo " PR #472 쓰기 경로 검증 — 태그 $tag"
fi
echo "=========================================="
echo "  대상  : $api"
echo "  구매자: $buyer"
echo "  판매자: $seller"
echo

# ── 사전 확인: 여기서 걸리면 롤아웃을 낭비하지 않는다 ──────────────
buyer_token="$(login "$buyer")"
seller_token="$(login "$seller")"
echo "  ✓ 로그인"

curl -fsS --max-time 15 "$api/stores/mine" -H "Authorization: Bearer $seller_token" > "$body"
store_count="$(pick "len(data)")" || fail "매장 목록을 읽지 못했습니다. 응답 형식이 바뀌었는지 확인하세요."
[[ "$store_count" != "0" ]] || fail "판매자 $seller 에게 매장이 없습니다. VERIFY_SELLER를 바꾸세요."
store_id="$(pick "data[0]['storeId']")" || fail "매장 ID를 읽지 못했습니다."

curl -fsS --max-time 15 "$api/stores/$store_id/dish" -H "Authorization: Bearer $seller_token" > "$body" \
  || fail "매장 $store_id 에 상품이 없습니다."
dish_id="$(pick "data['dishId']")" || fail "상품 ID를 읽지 못했습니다."
stock="$(pick "data['stockQuantity']")" || fail "재고를 읽지 못했습니다."
dish_status="$(pick "data['dishStatus']")" || fail "상품 상태를 읽지 못했습니다."
echo "  ✓ 매장 $store_id · 상품 $dish_id · 재고 $stock · 상태 $dish_status"
[[ "$stock" -ge 1 ]]            || fail "재고가 없습니다($stock). 주문을 만들 수 없습니다."
[[ "$dish_status" == "ON_SALE" ]] || fail "상품이 판매중이 아닙니다($dish_status)."

curl -fsS --max-time 15 "$api/deposits/balance" -H "Authorization: Bearer $buyer_token" > "$body"
balance="$(pick "data['balance']")" || fail "예치금 잔액를 읽지 못했습니다."
echo "  ✓ 구매자 예치금 $balance"
python3 -c "import sys; sys.exit(0 if float('$balance') > 0 else 1)" \
  || fail "예치금이 0입니다. 주문 생성이 실패합니다."

curl -fsS --max-time 15 "$api/carts/members" -H "Authorization: Bearer $buyer_token" > "$body"
cart_id="$(pick "data['cartId']")" || fail "장바구니 ID를 읽지 못했습니다."
existing="$(pick "len(data.get('items') or [])")" || fail "장바구니 항목 수를 읽지 못했습니다."
echo "  ✓ 장바구니 $cart_id · 담긴 항목 ${existing}건"
[[ "$existing" == "0" ]] || fail "장바구니에 ${existing}건이 남아 있습니다. 비우고 다시 실행하세요."

if [[ -n "$preflight_only" ]]; then
  cat <<'DONE'

  ✅ 사전 확인 통과 — 실측을 진행할 수 있습니다.

  다음 순서:
    1) 계측 켜기
       kubectl -n app set env deploy/core-service REQUEST_LOG_COUNT_SQL_STATEMENTS=true
       kubectl -n app rollout status deploy/core-service --timeout=600s
    2) 실측
       ./verify-write-path.sh after472
    3) 로그 판독 (스크립트가 명령을 찍어 줍니다)
    4) 계측 끄기
       kubectl -n app set env deploy/core-service REQUEST_LOG_COUNT_SQL_STATEMENTS-
       kubectl -n app rollout status deploy/core-service --timeout=600s
DONE
  exit 0
fi

echo
echo "  ⚠️  계측이 켜져 있어야 합니다(REQUEST_LOG_COUNT_SQL_STATEMENTS=true)."
read -r -p "  켜져 있으면 Enter, 중단은 Ctrl-C: " _
echo

# ── 실측: 여기부터 운영 데이터를 바꾼다 ──────────────────────────
echo "[1/4] 장바구니 담기"
curl -fsS --max-time 20 -X POST "$api/carts/$cart_id/items" \
  -H "Authorization: Bearer $buyer_token" -H 'Content-Type: application/json' \
  -H "X-Request-Id: $tag-cart-add" \
  -d "{\"dishId\":$dish_id,\"quantity\":1}" > "$body"
cart_item_id="$(pick "data['cartItemId']")" || fail "장바구니 항목 ID를 읽지 못했습니다."
price_version="$(pick "data.get('lastAppliedDishPriceVersion') or 0")" || fail "가격 버전를 읽지 못했습니다."
echo "      항목 $cart_item_id · priceVersion $price_version"

echo "[2/4] 주문 생성  ← 이번 수정의 주인공"
curl -fsS --max-time 20 -X POST "$api/orders/cartItems/$cart_item_id" \
  -H "Authorization: Bearer $buyer_token" -H 'Content-Type: application/json' \
  -H "X-Request-Id: $tag-order-create" \
  -d "{\"dishPriceVersion\":$price_version}" > "$body"
order_id="$(pick "data['orderId']")" || fail "주문 ID를 읽지 못했습니다."
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

queryCount 필드가 아예 안 보이면 계측이 꺼진 것이다. 켜고 다시 실행한다.

측정이 끝나면 계측을 반드시 끕니다.
  kubectl -n app set env deploy/core-service REQUEST_LOG_COUNT_SQL_STATEMENTS-
  kubectl -n app rollout status deploy/core-service --timeout=600s
──────────────────────────────────────────────
REPORT
