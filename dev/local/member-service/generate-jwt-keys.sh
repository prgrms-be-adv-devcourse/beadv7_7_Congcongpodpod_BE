#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MEMBER_KEY_DIR="$SCRIPT_DIR/keys"
GATEWAY_KEY_DIR="$SCRIPT_DIR/../gateway-service/keys"
TEMP_DIR="$MEMBER_KEY_DIR/.tmp-$$"

if ! command -v openssl >/dev/null 2>&1; then
  echo "오류: openssl이 설치되어 있지 않습니다." >&2
  exit 1
fi

if [ "${1:-}" != "--force" ]; then
  if [ -e "$MEMBER_KEY_DIR/access-private-key.pem" ] ||
    [ -e "$MEMBER_KEY_DIR/access-public-key.pem" ] ||
    [ -e "$GATEWAY_KEY_DIR/access-public-key.pem" ]; then
    echo "오류: 기존 Access Token 키가 있습니다. 재생성하려면 --force를 사용하세요." >&2
    exit 1
  fi
fi

mkdir -p "$TEMP_DIR"
trap 'rm -rf "$TEMP_DIR"' EXIT HUP INT TERM

private_key="$TEMP_DIR/access-private-key.pem"
public_key="$TEMP_DIR/access-public-key.pem"

echo "Access Token JWT 키 생성 중..."
openssl genpkey \
  -quiet \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "$private_key"
openssl pkey \
  -in "$private_key" \
  -pubout \
  -out "$public_key"

chmod 600 "$private_key"
chmod 644 "$public_key"

mkdir -p "$MEMBER_KEY_DIR" "$GATEWAY_KEY_DIR"
mv "$private_key" "$MEMBER_KEY_DIR/access-private-key.pem"
mv "$public_key" "$MEMBER_KEY_DIR/access-public-key.pem"
cp "$MEMBER_KEY_DIR/access-public-key.pem" "$GATEWAY_KEY_DIR/access-public-key.pem"
chmod 644 "$GATEWAY_KEY_DIR/access-public-key.pem"

echo "로컬 Access Token JWT 키 생성 완료"
echo "- Member: $MEMBER_KEY_DIR/access-private-key.pem"
echo "- Member: $MEMBER_KEY_DIR/access-public-key.pem"
echo "- Gateway: $GATEWAY_KEY_DIR/access-public-key.pem"
