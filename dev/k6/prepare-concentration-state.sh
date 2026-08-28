#!/usr/bin/env bash

# 현재 배포된 시드 데이터에서 집중도 A/B용 판매자 40명을 읽어 상태 파일을 만든다.
# DB는 조회만 하며, 결과 파일은 임시 파일을 완성한 뒤 한 번에 교체한다.

set -euo pipefail

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_PASSWORD가 필요합니다." >&2
  exit 2
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
output_file="${1:-$script_dir/results/r2-concentration-state.json}"
DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-5433}"
SEED_ACCOUNT_WIDTH="${SEED_ACCOUNT_WIDTH:-4}"
TARGET_FROM="${TARGET_FROM:-301}"
TARGET_TO="${TARGET_TO:-340}"

mkdir -p "$(dirname -- "$output_file")"
rows_file="$(mktemp "${TMPDIR:-/tmp}/lastdish-concentration-rows.XXXXXX")"
state_file="$(mktemp "${TMPDIR:-/tmp}/lastdish-concentration-state.XXXXXX")"
trap 'rm -f "$rows_file" "$state_file"' EXIT

PGPASSWORD="$DB_PASSWORD" docker run --rm -i -e PGPASSWORD postgres:16-alpine \
  psql -h "$DB_HOST" -p "$DB_PORT" -U core -d core_db -v ON_ERROR_STOP=1 -At -F '|' \
  -c "
    SELECT s.member_id, s.store_id, d.id
    FROM stores s
    JOIN dishes d ON d.store_id = s.store_id
    WHERE s.store_id BETWEEN $TARGET_FROM AND $TARGET_TO
      AND d.id = s.store_id
      AND s.status = 'OPEN'
      AND s.is_deleted = false
      AND d.dish_status = 'ON_SALE'
      AND d.is_deleted = false
    ORDER BY s.store_id;
  " > "$rows_file"

row_count="$(wc -l < "$rows_file" | tr -d ' ')"
if [ "$row_count" -ne 40 ]; then
  echo "집중도 대상이 40개가 아닙니다: actual=$row_count range=$TARGET_FROM~$TARGET_TO" >&2
  exit 1
fi

jq -Rn \
  --argjson width "$SEED_ACCOUNT_WIDTH" \
  '[inputs | split("|") | {memberId:(.[0]|tonumber), storeId:(.[1]|tonumber), dishId:(.[2]|tonumber)}]
   | [to_entries[]
      | .value + {
          key:("seed-" + (.value.storeId|tostring)),
          windowKey:(["dawn","morning","afternoon","night"][.key % 4]),
          slot:(.key + 1),
          email:("seller" + ((.value.memberId|tostring) | ("0" * ($width - length)) + .) + "@seed.lastdish.kr"),
          openTime:"00:00", closeTime:"23:59",
          pickupStartTime:"00:00", pickupEndTime:"23:59"
        }]
   | {
       schemaVersion:1,
       runId:"r2-concentration-current-seed",
       campaignDate:"20260828",
       campaignDay:1,
       datasetEpoch:"deployed-seed-current",
       reconstructedDataset:true,
       partial:false,
       sellers:.
     }' < "$rows_file" > "$state_file"

mv "$state_file" "$output_file"
echo "집중도 상태 파일 생성: $output_file (판매자 ${row_count}명)"
