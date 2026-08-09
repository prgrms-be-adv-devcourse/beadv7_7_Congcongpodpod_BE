#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ENV_FILE="$SCRIPT_DIR/.env"
TUNNEL_PID=""
KIBANA_STARTED="false"

cleanup() {
  if [[ "$KIBANA_STARTED" == "true" ]]; then
    docker compose --env-file "$ENV_FILE" --file "$SCRIPT_DIR/compose.yaml" down >/dev/null 2>&1 || true
  fi
  if [[ -n "$TUNNEL_PID" ]]; then
    kill "$TUNNEL_PID" 2>/dev/null || true
    wait "$TUNNEL_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

if [[ ! -f "$ENV_FILE" ]]; then
  echo "오류: $ENV_FILE 파일이 없습니다." >&2
  echo "infra/local/kibana/.env.example을 .env로 복사한 뒤 실제 토큰을 입력하세요." >&2
  exit 1
fi

# 토큰 누락·빈 값은 SSH 연결 전에 Compose 설정 검증 단계에서 차단합니다.
docker compose \
  --env-file "$ENV_FILE" \
  --file "$SCRIPT_DIR/compose.yaml" \
  config --quiet

echo "Data EC2 Elasticsearch SSH 터널을 여는 중..."
ssh -NT \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=60 \
  -L 127.0.0.1:19200:10.30.2.93:9200 \
  lastdish-data &
TUNNEL_PID=$!

for _ in {1..20}; do
  if ! kill -0 "$TUNNEL_PID" 2>/dev/null; then
    wait "$TUNNEL_PID"
  fi
  if nc -z 127.0.0.1 19200 >/dev/null 2>&1; then
    break
  fi
  sleep 0.25
done

if ! nc -z 127.0.0.1 19200 >/dev/null 2>&1; then
  echo "오류: localhost:19200 SSH 터널을 확인할 수 없습니다." >&2
  exit 1
fi

echo "Kibana 컨테이너를 시작하는 중..."
docker compose \
  --env-file "$ENV_FILE" \
  --file "$SCRIPT_DIR/compose.yaml" \
  up --detach
KIBANA_STARTED="true"

echo "Kibana 준비 상태를 확인하는 중..."
KIBANA_READY="false"
for _ in {1..120}; do
  status_code=$(curl --silent --output /dev/null --write-out '%{http_code}' \
    http://127.0.0.1:5601/api/status || true)
  if [[ "$status_code" == "200" ]]; then
    KIBANA_READY="true"
    break
  fi

  if ! docker compose --env-file "$ENV_FILE" --file "$SCRIPT_DIR/compose.yaml" \
    ps --status running --quiet kibana | grep -q .; then
    echo "오류: Kibana 컨테이너가 실행 중이 아닙니다." >&2
    docker compose --env-file "$ENV_FILE" --file "$SCRIPT_DIR/compose.yaml" \
      logs --tail 80 kibana >&2
    exit 1
  fi
  sleep 1
done

if [[ "$KIBANA_READY" != "true" ]]; then
  echo "오류: 120초 안에 Kibana가 준비되지 않았습니다." >&2
  docker compose --env-file "$ENV_FILE" --file "$SCRIPT_DIR/compose.yaml" \
    logs --tail 80 kibana >&2
  exit 1
fi

echo "Kibana 실행 완료: http://localhost:5601"
read -r -p "브라우저를 자동으로 열까요? [y/N]: " OPEN_BROWSER
case "$OPEN_BROWSER" in
  y|Y|yes|YES|Yes)
    if command -v open >/dev/null 2>&1; then
      open http://localhost:5601
    elif command -v xdg-open >/dev/null 2>&1; then
      xdg-open http://localhost:5601 >/dev/null 2>&1 || true
    else
      echo "브라우저 실행 명령을 찾지 못했습니다. http://localhost:5601 을 직접 여세요."
    fi
    ;;
  *)
    echo "브라우저에서 http://localhost:5601 을 직접 여세요."
    ;;
esac

echo "Kibana와 SSH 터널을 종료하려면 Ctrl+C를 누르세요."
while kill -0 "$TUNNEL_PID" 2>/dev/null; do
  sleep 1
done

echo "오류: SSH 터널이 종료됐습니다." >&2
exit 1
