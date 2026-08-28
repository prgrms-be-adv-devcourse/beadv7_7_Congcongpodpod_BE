#!/usr/bin/env bash

# order-stock-race 부하테스트의 사전 준비를 자동화한다.
#
#   1. 지정한 계정들의 장바구니에 대상 dish를 담는다
#   2. 대상 dish의 재고를 목표 수량으로 맞춘다
#   3. accounts.csv를 생성한다
#
# 재고를 나중에 맞추는 이유: 재고가 0이면 장바구니 담기 자체가 거부되므로,
# 장바구니를 먼저 채워야 목표 재고가 0인 경우까지 다룰 수 있다.
#
# 사용법:
#   ./seed-accounts.sh --dish 1 --stock 3 --accounts 5
#
# 시드 데이터 기준 기본값을 쓰므로, 로컬 스택에서는 위 한 줄이면 충분하다.

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

dish_id=""
target_stock=""
account_count=""
owner_email="seller001@seed.lastdish.kr"
account_start=2
password="LastDish!2026"
email_format="seller%03d@seed.lastdish.kr"
base_url="${BASE_URL:-}"
output_file="$script_dir/accounts.csv"

usage() {
  cat >&2 <<'EOF'
사용법: ./seed-accounts.sh --dish <dishId> --stock <목표재고> --accounts <계정수> [옵션]

필수:
  --dish       대상 dish id
  --stock      테스트 시작 시점의 목표 재고 수량
  --accounts   장바구니를 채울 계정 수 (k6의 VUS와 같거나 크게)

옵션:
  --owner      재고를 조정할 매장 주인 계정 (기본: seller001@seed.lastdish.kr)
               시드 데이터는 member N이 store N을 소유하므로 dish의 storeId에 맞춰야 한다
  --start      계정 번호 시작값 (기본: 2 — 1번은 주인 계정이라 건너뛴다)
  --password   공통 비밀번호 (기본: LastDish!2026)
  --base-url   게이트웨이 주소 (기본: 환경변수 BASE_URL 또는 .env의 값)
  --output     생성할 CSV 경로 (기본: dev/k6/accounts.csv)

예시:
  ./seed-accounts.sh --dish 1 --stock 3 --accounts 5
  ./seed-accounts.sh --dish 7 --stock 10 --accounts 30 --owner seller007@seed.lastdish.kr
EOF
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dish) dish_id="${2:-}"; shift 2 ;;
    --stock) target_stock="${2:-}"; shift 2 ;;
    --accounts) account_count="${2:-}"; shift 2 ;;
    --owner) owner_email="${2:-}"; shift 2 ;;
    --start) account_start="${2:-}"; shift 2 ;;
    --password) password="${2:-}"; shift 2 ;;
    --base-url) base_url="${2:-}"; shift 2 ;;
    --output) output_file="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "알 수 없는 옵션: $1" >&2; usage ;;
  esac
done

[[ -n "$dish_id" ]] || { echo "--dish는 필수입니다." >&2; usage; }
[[ -n "$target_stock" ]] || { echo "--stock은 필수입니다." >&2; usage; }
[[ -n "$account_count" ]] || { echo "--accounts는 필수입니다." >&2; usage; }

# BASE_URL이 인자로도 환경변수로도 없으면 k6와 같은 .env를 읽는다.
if [[ -z "$base_url" && -f "$script_dir/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$script_dir/.env"
  set +a
  base_url="${BASE_URL:-}"
fi
[[ -n "$base_url" ]] || { echo "BASE_URL을 찾을 수 없습니다. --base-url로 지정하거나 dev/k6/.env를 준비하세요." >&2; exit 2; }
base_url="${base_url%/}"

command -v python3 >/dev/null || { echo "python3가 필요합니다 (JSON 파싱용)." >&2; exit 2; }

# 응답 JSON에서 한 값을 꺼낸다. 실패하면 응답 본문을 그대로 보여주고 멈춘다.
extract() {
  local body="$1" path="$2" label="$3"
  local value
  value=$(printf '%s' "$body" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(1)
node = data
for key in '$path'.split('.'):
    if not isinstance(node, dict) or key not in node:
        sys.exit(1)
    node = node[key]
print(node)
" 2>/dev/null) || {
    echo "[실패] ${label} — 응답에서 ${path}를 찾지 못했습니다." >&2
    echo "  응답: ${body:0:400}" >&2
    exit 1
  }
  printf '%s' "$value"
}

login() {
  local email="$1" body
  body=$(curl -sS -X POST "$base_url/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}")
  extract "$body" "data.accessToken" "로그인($email)"
}

echo "대상: dish=$dish_id, 목표 재고=$target_stock, 계정 수=$account_count"
echo "게이트웨이: $base_url"
echo

# 1. 계정들의 장바구니에 대상 dish를 담는다.
#    addItem은 upsert라 재실행해도 중복이 쌓이지 않는다.
echo "[1/3] 장바구니 준비"
emails=()
last_index=$((account_start + account_count - 1))
for ((i = account_start; i <= last_index; i++)); do
  # shellcheck disable=SC2059
  email=$(printf "$email_format" "$i")
  token=$(login "$email")
  cart_body=$(curl -sS "$base_url/api/v1/carts/members" -H "Authorization: Bearer $token")
  cart_id=$(extract "$cart_body" "data.cartId" "장바구니 조회($email)")

  add_body=$(curl -sS -X POST "$base_url/api/v1/carts/$cart_id/items" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"dishId\": $dish_id, \"quantity\": 1}")
  cart_item_id=$(extract "$add_body" "data.cartItemId" "장바구니 담기($email)")

  emails+=("$email")
  echo "  $email — cartId=$cart_id, cartItemId=$cart_item_id"
done

# 2. 재고를 목표 수량으로 맞춘다.
#    조정 API는 절대값이 아니라 증감분을 받으므로 현재 재고를 먼저 읽어 차이를 구한다.
echo
echo "[2/3] 재고 조정"
dish_body=$(curl -sS "$base_url/api/v1/dishes/$dish_id")
current_stock=$(extract "$dish_body" "data.stockQuantity" "dish 조회($dish_id)")
delta=$((target_stock - current_stock))

if [[ "$delta" -eq 0 ]]; then
  echo "  현재 재고가 이미 $target_stock 입니다. 조정을 건너뜁니다."
else
  owner_token=$(login "$owner_email")
  adjust_body=$(curl -sS -X PATCH "$base_url/api/v1/dishes/$dish_id/stock" \
    -H "Authorization: Bearer $owner_token" -H 'Content-Type: application/json' \
    -d "{\"quantityDelta\": $delta}")
  new_stock=$(extract "$adjust_body" "data.stockQuantity" "재고 조정(delta=$delta)")
  echo "  재고 $current_stock -> $new_stock (delta $delta, 주인 계정 $owner_email)"
fi

# 3. k6가 읽을 계정 목록을 만든다.
echo
echo "[3/3] $output_file 생성"
{
  echo "email,password"
  for email in "${emails[@]}"; do
    echo "$email,$password"
  done
} > "$output_file"
echo "  계정 ${#emails[@]}건 기록"

echo
echo "준비 완료. 이제 아래로 실행하세요:"
echo "  ./k6.sh order-stock-race -e DISH_ID=$dish_id -e VUS=$account_count"
echo
echo "주의: 주문에 성공한 장바구니 항목은 삭제됩니다. 재실행하려면 이 스크립트를 다시 돌리세요."
