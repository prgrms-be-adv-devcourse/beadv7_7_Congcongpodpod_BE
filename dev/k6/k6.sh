#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
env_file="$script_dir/.env"

if [[ ! -f "$env_file" ]]; then
  echo "dev/k6/.env가 없습니다. .env.example을 복사해 주세요." >&2
  exit 2
fi

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

command="run"
if [[ "${1:-}" == "inspect" ]]; then
  command="inspect"
  shift
fi
k6_args=("$command")
if [[ "$command" == "inspect" ]]; then
  k6_args+=(--include-system-env-vars)
fi

scenario="${1:-smoke}"
if [[ $# -gt 0 ]]; then
  shift
fi

scenario_file="$script_dir/$scenario.js"
if [[ ! -f "$scenario_file" ]]; then
  echo "알 수 없는 시나리오입니다: $scenario" >&2
  echo "사용 가능한 시나리오:" >&2
  find "$script_dir" -maxdepth 1 -name '*.js' -exec basename {} .js \; | sort >&2
  exit 2
fi

# .env에 있는 값만 컨테이너로 넘긴다. 없는 값은 스크립트의 기본값을 쓴다.
env_args=()
for key in BASE_URL SEED_PASSWORD THINK_MIN THINK_MAX REQUEST_TIMEOUT \
  SELLER_ORDER_RETRY SELLER_ORDER_RETRY_WAIT BUYER_ACCOUNT SELLER_ACCOUNT \
  PROBE_FROM PROBE_TO IGNORE_ORDER_WINDOW CAMPAIGN_DATE CAMPAIGN_DAY \
  DATASET_EPOCH RUN_ID STATE_FILE LOADTEST_PASSWORD SLOTS_PER_WINDOW \
  PREPARE_LIMIT PREPARE_SHARDS PREPARE_SHARD_INDEX MIN_BUYER_BALANCE; do
  if [[ -n "${!key:-}" ]]; then
    env_args+=(-e "$key=${!key}")
  fi
done

k6_image="${K6_IMAGE:-grafana/k6@sha256:5221b620a4f874faff6e32ba597aa667c058391fe4898b1c6f6377f062c6cdec}"

docker run --rm \
  -i \
  -v "$script_dir:/scripts:ro" \
  "${env_args[@]}" \
  "$k6_image" "${k6_args[@]}" "$@" "/scripts/$scenario.js"
