#!/usr/bin/env bash
#
# 부하 전후의 DB 스캔 통계를 찍어 두고 그 차이를 본다.
#
# 왜 델타인가: pg_stat_user_tables의 값은 마지막 초기화 이후 **누적**이라 한 번만 봐서는
# "이번 부하에서 무슨 일이 있었나"를 알 수 없다. pg_stat_reset()으로 0에서 시작하는 방법도
# 있지만 그러면 과거 값과의 비교가 영영 불가능해진다. 그래서 초기화하지 않고 전후 스냅샷의
# 차이만 본다 — 읽기 전용이고 되돌릴 것도 없다.
#
# 사용법:
#   ssh -f -N -L 5433:10.30.2.93:5433 lastdish-data     # 터널 (한 번만)
#   read -rs DB_PASSWORD && export DB_PASSWORD
#
#   ./measure-index-effect.sh snapshot before
#   ./run-concentration-ab.sh uniform 1                  # 부하 (12분)
#   ./measure-index-effect.sh snapshot after
#   ./measure-index-effect.sh diff
#
# 판정 (2026-08-29 배포된 V14 인덱스)
#   dishes  순차스캔 증가폭이 거의 0이고 idx_dishes_store_id 사용이 크게 늘면 인덱스가 먹은 것이다.
#           반대로 순차스캔만 늘면 플래너가 인덱스를 안 고르고 있다는 뜻이다.

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cmd="${1:-}"
label="${2:-}"
out_dir="$script_dir/results"
mkdir -p "$out_dir"

DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-5433}"
DB_USER="${DB_USER:-core}"
DB_NAME="${DB_NAME:-core_db}"

usage() {
  cat >&2 <<'USAGE'
사용법:
  ./measure-index-effect.sh snapshot before   부하 전 스냅샷
  ./measure-index-effect.sh snapshot after    부하 후 스냅샷
  ./measure-index-effect.sh diff              두 스냅샷의 차이
USAGE
  exit 2
}

run_psql() {
  [[ -n "${DB_PASSWORD:-}" ]] || { echo "DB_PASSWORD가 필요합니다." >&2; exit 2; }
  docker run --rm -i -e PGPASSWORD="$DB_PASSWORD" postgres:16-alpine \
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
      -v ON_ERROR_STOP=1 --csv -t -A -c "$1"
}

# 테이블 스캔 통계와 인덱스 사용 횟수를 한 형식으로 뽑는다.
# 종류|이름|지표1|지표2 로 맞춰 두면 diff가 단순해진다.
snapshot_sql() {
  cat <<'SQL'
SELECT 'table', relname, seq_scan::text, seq_tup_read::text, idx_scan::text, n_live_tup::text
  FROM pg_stat_user_tables
 WHERE relname IN ('dishes','stores','orders','cart_items','outbox_events','deposits','carts')
UNION ALL
SELECT 'index', relname || ' / ' || indexrelname, idx_scan::text, idx_tup_read::text, '', ''
  FROM pg_stat_user_indexes
 WHERE relname IN ('dishes','stores','orders','cart_items','outbox_events')
ORDER BY 1, 2
SQL
}

case "$cmd" in
  snapshot)
    [[ "$label" == "before" || "$label" == "after" ]] || usage
    f="$out_dir/pgstat-$label.csv"
    run_psql "$(snapshot_sql)" > "$f"
    echo "✓ $label 스냅샷 → $f  ($(wc -l < "$f" | tr -d ' ')줄)"
    echo "  찍은 시각: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    ;;

  diff)
    b="$out_dir/pgstat-before.csv"; a="$out_dir/pgstat-after.csv"
    [[ -f "$b" ]] || { echo "before 스냅샷이 없습니다: $b" >&2; exit 2; }
    [[ -f "$a" ]] || { echo "after 스냅샷이 없습니다: $a" >&2; exit 2; }
    python3 - "$b" "$a" <<'PY'
import csv, sys

def load(path):
    out = {}
    with open(path, encoding="utf-8") as f:
        for row in csv.reader(f):
            if len(row) < 6:
                continue
            kind, name = row[0], row[1]
            nums = [int(v) if v.strip() else 0 for v in row[2:6]]
            out[(kind, name)] = nums
    return out

before, after = load(sys.argv[1]), load(sys.argv[2])

def line(ch): print(ch * 78)

print()
line("=")
print(" 테이블 스캔 — 부하 동안의 증가분")
line("=")
print(f"{'테이블':<16}{'순차스캔':>10}{'순차로읽은행':>16}{'인덱스스캔':>12}{'현재행수':>12}")
line("-")
for (kind, name), a in sorted(after.items()):
    if kind != "table":
        continue
    b = before.get((kind, name), [0, 0, 0, 0])
    d = [a[i] - b[i] for i in range(3)]
    print(f"{name:<16}{d[0]:>+10,}{d[1]:>+16,}{d[2]:>+12,}{a[3]:>12,}")

print()
line("=")
print(" 인덱스 사용 — 부하 동안의 증가분 (0이면 이번 부하에서 안 쓰였다)")
line("=")
print(f"{'인덱스':<52}{'사용횟수':>10}{'읽은행':>14}")
line("-")
rows = []
for (kind, name), a in after.items():
    if kind != "index":
        continue
    b = before.get((kind, name), [0, 0, 0, 0])
    rows.append((a[0] - b[0], name, a[1] - b[1]))
for used, name, tup in sorted(rows, reverse=True):
    mark = "  " if used > 0 else " ·"
    print(f"{mark}{name:<50}{used:>+10,}{tup:>+14,}")

print()
line("=")
print(" 판정")
line("=")
d_t = after.get(("table", "dishes"), [0]*4)
b_t = before.get(("table", "dishes"), [0]*4)
seq, idx = d_t[0] - b_t[0], d_t[2] - b_t[2]
v14 = [r for r in rows if "idx_dishes_store_id" in r[1]]
v14_used = v14[0][0] if v14 else 0

print(f"  dishes 순차스캔 증가 : {seq:+,}")
print(f"  dishes 인덱스스캔 증가: {idx:+,}")
print(f"  idx_dishes_store_id  : {v14_used:+,}회 사용")
print()
if v14_used > 0 and seq <= idx:
    print("  ✅ V14 인덱스가 실제로 쓰였고 순차 스캔이 인덱스 스캔을 넘지 않는다.")
elif v14_used > 0:
    print("  ⚠️  인덱스가 쓰이긴 했으나 순차 스캔이 더 많다. 어떤 쿼리가 순차로 가는지 봐야 한다.")
elif seq == 0 and idx == 0:
    print("  ⚠️  dishes에 아무 조회도 없었다. 부하 시나리오가 상품을 안 읽었는지 확인한다.")
else:
    print("  ❌ V14 인덱스가 한 번도 쓰이지 않았다. 플래너가 순차 스캔을 고르고 있다.")
print()
print("  주의: 이 값은 부하 중 다른 트래픽(스케줄러·실사용자)도 포함한다. 단독 효과가 아니다.")
PY
    ;;

  *) usage ;;
esac
