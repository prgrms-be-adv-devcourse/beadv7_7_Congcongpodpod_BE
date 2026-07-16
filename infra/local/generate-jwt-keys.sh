#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
KEY_DIR="$SCRIPT_DIR/keys"
TEMP_DIR="$KEY_DIR/.tmp-$$"

if ! command -v openssl >/dev/null 2>&1; then
  echo "오류: openssl이 설치되어 있지 않습니다." >&2
  exit 1
fi

if [ "${1:-}" != "--force" ]; then
  for key_type in access refresh; do
    if [ -e "$KEY_DIR/$key_type-private-key.pem" ] || [ -e "$KEY_DIR/$key_type-public-key.pem" ]; then
      echo "오류: 기존 키가 있습니다. 재생성하려면 --force를 사용하세요." >&2
      exit 1
    fi
  done
fi

mkdir -p "$TEMP_DIR"
trap 'rm -rf "$TEMP_DIR"' EXIT HUP INT TERM

for key_type in access refresh; do
  private_key="$TEMP_DIR/$key_type-private-key.pem"
  public_key="$TEMP_DIR/$key_type-public-key.pem"

  echo "$key_type JWT 키 생성 중..."
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
done

mkdir -p "$KEY_DIR"
for key_type in access refresh; do
  mv "$TEMP_DIR/$key_type-private-key.pem" "$KEY_DIR/$key_type-private-key.pem"
  mv "$TEMP_DIR/$key_type-public-key.pem" "$KEY_DIR/$key_type-public-key.pem"
done

echo "로컬 JWT 키 생성 완료: $KEY_DIR"
echo "- Gateway: access-public-key.pem"
echo "- Member: access/refresh private-key.pem"
