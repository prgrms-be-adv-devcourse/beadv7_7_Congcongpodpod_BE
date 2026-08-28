#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
env_file="$script_dir/.env"

if [[ ! -f "$env_file" ]]; then
  echo "dev/k6/.env가 없습니다. .env.example을 복사해 주세요." >&2
  exit 2
fi

# 명령 앞 PREPARE_LIMIT=1 같은 당일 검증값이 .env의 빈 기본값에 덮이지 않게 보존한다.
prepare_limit_was_set="${PREPARE_LIMIT+x}"
prepare_limit_override="${PREPARE_LIMIT:-}"

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

if [[ -n "$prepare_limit_was_set" ]]; then
  PREPARE_LIMIT="$prepare_limit_override"
  export PREPARE_LIMIT
fi

required_commands=(jq grep tail mktemp)
for required_command in "${required_commands[@]}"; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "필수 명령을 찾을 수 없습니다: $required_command" >&2
    exit 2
  fi
done

if [[ -z "${RUN_ID:-}" || ! "$RUN_ID" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "RUN_ID는 파일명에 안전한 값이어야 합니다: ${RUN_ID:-}" >&2
  exit 2
fi
if [[ ! "${CAMPAIGN_DAY:-}" =~ ^[1-5]$ ]]; then
  echo "CAMPAIGN_DAY는 1~5 정수여야 합니다: ${CAMPAIGN_DAY:-}" >&2
  exit 2
fi
if [[ ! "${PREPARE_SHARDS:-4}" =~ ^[1-9][0-9]*$ ]]; then
  echo "PREPARE_SHARDS는 1 이상의 정수여야 합니다: ${PREPARE_SHARDS:-}" >&2
  exit 2
fi
if [[ -n "${PREPARE_LIMIT:-}" && ! "$PREPARE_LIMIT" =~ ^[1-9][0-9]*$ ]]; then
  echo "PREPARE_LIMIT은 비어 있거나 1 이상의 정수여야 합니다: $PREPARE_LIMIT" >&2
  exit 2
fi
if [[ -z "${LOADTEST_PASSWORD:-}" || "$LOADTEST_PASSWORD" == "change-me-before-data-creation" ]]; then
  echo "LOADTEST_PASSWORD를 실제 준비용 값으로 설정해야 합니다." >&2
  exit 2
fi

results_dir="$script_dir/results"
mkdir -p "$results_dir"

echo "시드 member_snapshots 300개와 구매자 예치금 시작 조건을 확인합니다."
"$script_dir/k6.sh" probe-member-snapshot

shard_count="${PREPARE_SHARDS:-4}"
pids=()
logs=()

for ((shard = 0; shard < shard_count; shard += 1)); do
  log_file="$results_dir/${RUN_ID}-prepare-${shard}.log"
  logs+=("$log_file")
  k6_arguments=(
    prepare-daily-state
    --log-format raw
    -e "PREPARE_SHARDS=$shard_count"
    -e "PREPARE_SHARD_INDEX=$shard"
  )
  if [[ -n "${PREPARE_LIMIT:-}" ]]; then
    k6_arguments+=(-e "PREPARE_LIMIT=$PREPARE_LIMIT")
  fi
  "$script_dir/k6.sh" "${k6_arguments[@]}" >"$log_file" 2>&1 &
  pids+=("$!")
done

prepare_failed=0
for index in "${!pids[@]}"; do
  if ! wait "${pids[$index]}"; then
    echo "prepare shard $index 실패: ${logs[$index]}" >&2
    tail -n 30 "${logs[$index]}" >&2
    prepare_failed=1
  fi
done
if [[ "$prepare_failed" -ne 0 ]]; then
  exit 1
fi

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/ld273-prepare.XXXXXX")"
trap 'rm -rf "$temporary_dir"' EXIT

for ((shard = 0; shard < shard_count; shard += 1)); do
  manifest_line="$(grep '^LD273_MANIFEST=' "${logs[$shard]}" | tail -n 1)"
  if [[ -z "$manifest_line" ]]; then
    echo "prepare shard $shard 로그에 LD273_MANIFEST가 없습니다." >&2
    exit 1
  fi
  printf '%s\n' "${manifest_line#LD273_MANIFEST=}" >"$temporary_dir/$shard.json"
done

full_expected_count=$((40 * CAMPAIGN_DAY))
expected_count="$full_expected_count"
partial=false
output_file="$results_dir/${RUN_ID}-state.json"
if [[ -n "${PREPARE_LIMIT:-}" ]]; then
  partial=true
  if ((PREPARE_LIMIT < full_expected_count)); then
    expected_count="$PREPARE_LIMIT"
  fi
  output_file="$results_dir/${RUN_ID}-partial-state.json"
fi

jq -s \
  --arg run_id "$RUN_ID" \
  --arg campaign_date "$CAMPAIGN_DATE" \
  --arg dataset_epoch "$DATASET_EPOCH" \
  --argjson campaign_day "$CAMPAIGN_DAY" \
  --argjson shard_count "$shard_count" \
  --argjson expected_count "$expected_count" \
  --argjson partial "$partial" \
  '
    if length != $shard_count then error("shard manifest 개수가 다릅니다") else . end
    | if (map(.shardIndex) | sort) != [range(0; $shard_count)]
      then error("shard index가 0부터 연속되지 않습니다") else . end
    | if all(.[];
        .schemaVersion == 1 and
        .runId == $run_id and
        .campaignDate == $campaign_date and
        .campaignDay == $campaign_day and
        .datasetEpoch == $dataset_epoch and
        .reconstructedDataset == true and
        .partial == $partial and
        .shardCount == $shard_count)
      then . else error("shard manifest 메타데이터가 서로 다릅니다") end
    | map(.sellers[]) as $sellers
    | if ($sellers | length) != $expected_count
      then error("판매자 목표 개수가 다릅니다") else . end
    | if ($sellers | map(.email) | unique | length) != $expected_count
      then error("판매자 이메일이 중복됩니다") else . end
    | if ($sellers | map(.memberId) | unique | length) != $expected_count
      then error("memberId가 중복됩니다") else . end
    | if ($sellers | map(.storeId) | unique | length) != $expected_count
      then error("storeId가 중복됩니다") else . end
    | if ($sellers | map(.dishId) | unique | length) != $expected_count
      then error("dishId가 중복됩니다") else . end
    | {
        schemaVersion: 1,
        runId: $run_id,
        campaignDate: $campaign_date,
        campaignDay: $campaign_day,
        datasetEpoch: $dataset_epoch,
        reconstructedDataset: true,
        partial: $partial,
        shardIndexes: (map(.shardIndex) | sort),
        sellers: $sellers
      }
  ' "$temporary_dir"/*.json >"$output_file"

if grep -Eq '"(password|accessToken|refreshToken)"' "$output_file"; then
  echo "상태 파일에 비밀 필드가 포함돼 있습니다: $output_file" >&2
  exit 1
fi

echo "당일 상태 파일 생성: $output_file"
