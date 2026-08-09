#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
env_file="$script_dir/.env"
scenario="${1:-smoke}"

if [[ $# -gt 0 ]]; then
  shift
fi

if [[ ! -f "$env_file" ]]; then
  echo "dev/k6/.env가 없습니다. .env.example을 복사해 주세요." >&2
  exit 2
fi

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

scenario_file="$script_dir/$scenario.js"
if [[ ! -f "$scenario_file" ]]; then
  echo "알 수 없는 시나리오입니다: $scenario" >&2
  echo "사용 가능한 시나리오:" >&2
  find "$script_dir" -maxdepth 1 -name '*.js' -exec basename {} .js \; | sort >&2
  exit 2
fi

docker run --rm \
  -i \
  -v "$script_dir:/scripts:ro" \
  -e BASE_URL="$BASE_URL" \
  grafana/k6 run "$@" "/scripts/$scenario.js"
