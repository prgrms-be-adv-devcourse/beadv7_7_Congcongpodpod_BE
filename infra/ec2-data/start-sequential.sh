#!/usr/bin/env bash
set -euo pipefail

DATABASE_DIR=/home/ec2-user/database
COMPOSE=(docker compose --env-file "$DATABASE_DIR/.env" --file "$DATABASE_DIR/compose.yaml")
HEALTH_TIMEOUT_SECONDS=300

wait_for_health() {
  local service_name=$1
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  local container_id
  local health

  container_id=$("${COMPOSE[@]}" ps --quiet "$service_name")
  if [[ -z "$container_id" ]]; then
    echo "$service_name container was not created." >&2
    return 1
  fi

  until [[ $SECONDS -ge $deadline ]]; do
    health=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")
    if [[ "$health" == "healthy" ]]; then
      echo "$service_name is healthy."
      return 0
    fi
    if [[ "$health" == "exited" || "$health" == "dead" ]]; then
      echo "$service_name stopped before becoming healthy." >&2
      return 1
    fi
    sleep 5
  done

  echo "$service_name did not become healthy within ${HEALTH_TIMEOUT_SECONDS}s." >&2
  return 1
}

cd "$DATABASE_DIR"
"${COMPOSE[@]}" config --quiet

"${COMPOSE[@]}" up --detach member-postgresql
wait_for_health member-postgresql

"${COMPOSE[@]}" up --detach core-postgresql
wait_for_health core-postgresql

"${COMPOSE[@]}" up --detach database-initializer
initializer_id=$("${COMPOSE[@]}" ps --all --quiet database-initializer)
initializer_exit_code=$(docker wait "$initializer_id")
if [[ "$initializer_exit_code" != "0" ]]; then
  echo "database-initializer failed with exit code $initializer_exit_code." >&2
  exit 1
fi
echo "database-initializer completed successfully."

"${COMPOSE[@]}" up --detach elasticsearch
wait_for_health elasticsearch

"${COMPOSE[@]}" up --detach
echo "All Data EC2 services started."
