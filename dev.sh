#!/usr/bin/env bash

set -euo pipefail

# 어느 디렉터리에서 호출해도 루트 compose.yaml을 사용합니다.
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_root"

command="${1:-up}"
if [[ $# -gt 0 ]]; then
  shift
fi

case "$command" in
  down)
    # 컨테이너와 네트워크만 제거합니다. 볼륨 데이터와 이미지는 유지됩니다.
    docker compose down "$@"
    exit 0
    ;;
  stop)
    # 지정한 서비스만 중지합니다. 서비스명이 없으면 전체 서비스를 중지합니다.
    docker compose stop "$@"
    exit 0
    ;;
  reset)
    target="${1:-}"
    case "$target" in
      member-db|core-db|payment-db|ai-db|kafka|redis|elasticsearch|all) ;;
      *)
        echo "초기화 대상을 지정하세요: member-db, core-db, payment-db, ai-db, kafka, redis, elasticsearch, all" >&2
        exit 2
        ;;
    esac

    echo "WARNING: '$target'의 로컬 데이터가 복구할 수 없게 삭제됩니다."
    read -r -p "계속하려면 RESET $target 을 입력하세요: " confirmation
    if [[ "$confirmation" != "RESET $target" ]]; then
      echo "초기화를 취소했습니다."
      exit 1
    fi

    case "$target" in
      member-db)
        docker compose stop member-service
        docker compose up -d member-db
        docker compose exec -T member-db psql -v ON_ERROR_STOP=1 -U member -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'member_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS member_db;" \
          -c "CREATE DATABASE member_db OWNER member;"
        docker compose up -d member-service
        ;;
      core-db)
        docker compose stop core-service
        docker compose up -d core-db
        docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'core_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS core_db;" \
          -c "CREATE DATABASE core_db OWNER core;"
        docker compose up -d core-service
        ;;
      payment-db)
        docker compose stop payment-service
        docker compose up -d core-db database-initializer
        docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'payment_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS payment_db;" \
          -c "CREATE DATABASE payment_db OWNER payment;"
        docker compose up -d payment-service
        ;;
      ai-db)
        docker compose stop ai-service
        docker compose up -d core-db database-initializer
        docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'ai_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS ai_db;" \
          -c "CREATE DATABASE ai_db OWNER ai;"
        docker compose up -d ai-service
        ;;
      kafka)
        docker compose stop kafka
        docker compose rm -f kafka
        docker volume rm lastdish-local_kafka-data 2>/dev/null || true
        docker compose up -d kafka
        ;;
      redis)
        docker compose up -d redis
        docker compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli FLUSHALL'
        ;;
      elasticsearch)
        docker compose stop elasticsearch
        docker compose rm -f elasticsearch
        docker volume rm lastdish-local_elasticsearch-data 2>/dev/null || true
        docker compose up -d elasticsearch
        ;;
      all)
        # 전체 컨테이너를 제거한 뒤 모든 영속 데이터 볼륨을 삭제합니다.
        # Redis는 비영속 구성이므로 컨테이너 제거만으로 데이터가 초기화됩니다.
        docker compose down
        docker volume rm \
          lastdish-local_member-db-data \
          lastdish-local_core-db-data \
          lastdish-local_kafka-data \
          lastdish-local_elasticsearch-data 2>/dev/null || true
        docker compose up -d --build
        ;;
    esac
    echo "'$target' 초기화를 완료했습니다."
    exit 0
    ;;
  up)
    ;;
  -h|--help|help)
    echo "Usage: ./dev.sh [up] [service ...]"
    echo "       ./dev.sh stop [service ...]"
    echo "       ./dev.sh down"
    echo "       ./dev.sh reset <member-db|core-db|payment-db|ai-db|kafka|redis|elasticsearch|all>"
    exit 0
    ;;
  *)
    # 서비스명만 전달하는 간단한 사용법(./dev.sh member-service)을 지원합니다.
    set -- "$command" "$@"
    ;;
esac

# 빌드 전 현재 LastDish Compose 컨테이너가 참조하는 이미지 ID를 기록합니다.
# 빌드 실패 시 즉시 종료되므로 기존 이미지는 삭제되지 않습니다.
before_images="$(docker compose images -q 2>/dev/null | sort -u || true)"

docker compose up -d --build "$@"

# 새 컨테이너가 참조하는 이미지는 보존하고, 이번 빌드로 교체된 이전 이미지만 제거합니다.
after_images="$(docker compose images -q | sort -u)"
while IFS= read -r image_id; do
  [[ -z "$image_id" ]] && continue
  if ! grep -Fqx "$image_id" <<<"$after_images"; then
    # 다른 컨테이너가 사용 중이면 Docker가 삭제를 거부하므로 강제 삭제하지 않습니다.
    docker image rm "$image_id" || true
  fi
done <<<"$before_images"
