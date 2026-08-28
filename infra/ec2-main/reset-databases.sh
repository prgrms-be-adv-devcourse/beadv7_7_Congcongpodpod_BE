#!/usr/bin/env bash

set -Eeuo pipefail

namespace="app"
reset_job="database-reset-$(date +%s)"
all_services=(member-service core-service payment-service ai-service gateway-service)
services=()
stop_order=()
original_replicas=()

usage() {
  cat <<'EOF'
사용법:
  ./reset-databases.sh help
  ./reset-databases.sh <대상> --confirm [--defer-flyway]

대상:
  all          전체 DB를 초기화합니다.
  member-db    member_db만 초기화합니다.
  core-db      core_db만 초기화합니다.
  payment-db   payment_db만 초기화합니다.
  ai-db        ai_db만 초기화합니다.

기본 동작:
  1. 대상 DB를 사용하는 서비스를 중지합니다.
  2. public 스키마를 삭제하고 빈 public 스키마를 다시 생성합니다.
  3. 기존 replica 수로 서비스를 복구합니다.
  4. Spring Boot 시작 과정에서 Flyway migration과 seed를 적용합니다.

  all 복구 순서:
    Member Service → Core Service → Payment Service → AI Service → Gateway Service
    각 서비스가 Ready 상태가 된 후 다음 서비스를 시작합니다.

필수 옵션:
  --confirm
      데이터 삭제를 확인합니다. 이 옵션이 없으면 실행하지 않습니다.

선택 옵션:
  --defer-flyway
      DB만 초기화하고 서비스를 중지하거나 재시작하지 않습니다.
      Flyway는 영향받은 서비스가 다음에 재시작 또는 배포될 때 실행됩니다.
      그전까지 실행 중인 서비스에서 DB 스키마 오류가 발생할 수 있습니다.

도움말:
  help, --help, -h
      이 도움말을 출력하며 DB를 변경하지 않습니다.

예시:
  ./reset-databases.sh member-db --confirm
      member_db 초기화 후 Member Service를 복구하고 Flyway를 즉시 실행합니다.

  ./reset-databases.sh core-db --confirm --defer-flyway
      core_db만 초기화합니다. Core Service와 Flyway는 다음 배포까지 유지됩니다.

  ./reset-databases.sh all --confirm
      전체 DB 초기화 후 서비스를 최대 2개씩 복구하며 Flyway를 실행합니다.

  ./reset-databases.sh all --confirm --defer-flyway
      전체 DB만 초기화합니다. 이후 모든 관련 서비스를 재시작 또는 배포해야 합니다.

주의: DB 초기화는 복구할 수 없습니다. 실행 전 백업 필요 여부를 확인하십시오.
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

cleanup() {
  kubectl delete job "$reset_job" -n "$namespace" --ignore-not-found >/dev/null 2>&1 || true
}

on_error() {
  local exit_code=$?
  if [[ "${defer_flyway:-false}" == "true" ]]; then
    printf '[ERROR] Database-only reset failed. Applications were not intentionally stopped or restarted.\n' >&2
  else
    printf '[ERROR] Database reset stopped. Applications may remain scaled down or partially restored.\n' >&2
  fi
  printf '[ERROR] Check status: kubectl get deployments,pods -n %s\n' "$namespace" >&2
  exit "$exit_code"
}

trap cleanup EXIT
trap on_error ERR

if [[ $# -eq 1 ]]; then
  case "$1" in
    help|--help|-h)
      usage
      exit 0
      ;;
  esac
fi

if [[ $# -lt 2 || $# -gt 3 || "$2" != "--confirm" ]]; then
  usage
  exit 2
fi

defer_flyway=false
if [[ $# -eq 3 ]]; then
  if [[ "$3" != "--defer-flyway" ]]; then
    usage
    exit 2
  fi
  defer_flyway=true
fi

target="$1"
case "$target" in
  all)
    services=("${all_services[@]}")
    stop_order=(gateway-service ai-service payment-service core-service member-service)
    ;;
  member-db)
    services=(member-service)
    stop_order=(member-service)
    ;;
  core-db)
    services=(core-service)
    stop_order=(core-service)
    ;;
  payment-db)
    services=(payment-service)
    stop_order=(payment-service)
    ;;
  ai-db)
    services=(ai-service)
    stop_order=(ai-service)
    ;;
  *)
    usage
    exit 2
    ;;
esac

command -v kubectl >/dev/null 2>&1 || fail 'kubectl is not installed.'
kubectl auth can-i create jobs.batch -n "$namespace" | grep -qx yes \
  || fail "Current Kubernetes user cannot create Jobs in namespace ${namespace}."

for resource in configmap/app-runtime-config secret/app-runtime-secrets; do
  kubectl get "$resource" -n "$namespace" >/dev/null \
    || fail "Required resource is missing: ${namespace}/${resource}"
done

for service in "${services[@]}"; do
  kubectl get deployment "$service" -n "$namespace" >/dev/null \
    || fail "Required deployment is missing: ${namespace}/${service}"
  original_replicas+=("$(
    kubectl get deployment "$service" -n "$namespace" -o jsonpath='{.spec.replicas}'
  )")
done

if kubectl get jobs -n "$namespace" -l app=database-reset \
  -o jsonpath='{range .items[?(@.status.completionTime==null)]}{.metadata.name}{"\n"}{end}' \
  | grep -q .; then
  fail 'Another database reset Job is already active.'
fi

if [[ "$defer_flyway" == "true" ]]; then
  log 'WARNING: Database-only mode is enabled.'
  log 'WARNING: Running services are not restarted and can fail until their next deployment or restart.'
  log 'WARNING: Every service affected by the selected target must be restarted or deployed.'
else
  log "Stopping services affected by ${target}."
  for service in "${stop_order[@]}"; do
    kubectl scale deployment "$service" -n "$namespace" --replicas=0 >/dev/null
    log "Stopped ${service}."
  done

  for service in "${services[@]}"; do
    kubectl wait --for=delete pod -l "app=${service}" -n "$namespace" --timeout=180s >/dev/null 2>&1 || true
  done
fi

log 'Creating the database reset Job.'
kubectl apply -f - <<EOF >/dev/null
apiVersion: batch/v1
kind: Job
metadata:
  name: ${reset_job}
  namespace: ${namespace}
  labels:
    app: database-reset
spec:
  backoffLimit: 0
  template:
    metadata:
      labels:
        app: database-reset
    spec:
      restartPolicy: Never
      containers:
        - name: reset
          image: postgres:17-alpine
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh", "-ec"]
          args:
            - |
              reset_schema() {
                label="\$1"
                jdbc_url="\$2"
                username="\$3"
                password="\$4"
                database_url="\${jdbc_url#jdbc:}"

                echo "Resetting \${label}."
                PGPASSWORD="\$password" psql "\$database_url" \
                  --username="\$username" \
                  --set=ON_ERROR_STOP=1 \
                  --command='DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;'
                echo "Reset \${label}."
              }

              case "\$RESET_TARGET" in
                all)
                  reset_schema member_db "\$MEMBER_DB_URL" "\$MEMBER_DB_USERNAME" "\$MEMBER_DB_PASSWORD"
                  reset_schema core_db "\$CORE_DB_URL" "\$CORE_DB_USERNAME" "\$CORE_DB_PASSWORD"
                  reset_schema payment_db "\$PAYMENT_DB_URL" "\$PAYMENT_DB_USERNAME" "\$PAYMENT_DB_PASSWORD"
                  reset_schema ai_db "\$AI_DB_URL" "\$AI_DB_USERNAME" "\$AI_DB_PASSWORD"
                  ;;
                member-db)
                  reset_schema member_db "\$MEMBER_DB_URL" "\$MEMBER_DB_USERNAME" "\$MEMBER_DB_PASSWORD"
                  ;;
                core-db)
                  reset_schema core_db "\$CORE_DB_URL" "\$CORE_DB_USERNAME" "\$CORE_DB_PASSWORD"
                  ;;
                payment-db)
                  reset_schema payment_db "\$PAYMENT_DB_URL" "\$PAYMENT_DB_USERNAME" "\$PAYMENT_DB_PASSWORD"
                  ;;
                ai-db)
                  reset_schema ai_db "\$AI_DB_URL" "\$AI_DB_USERNAME" "\$AI_DB_PASSWORD"
                  ;;
                *)
                  echo "Unknown reset target: \$RESET_TARGET" >&2
                  exit 2
                  ;;
              esac
          env:
            - name: RESET_TARGET
              value: ${target}
            - name: MEMBER_DB_URL
              valueFrom: {configMapKeyRef: {name: app-runtime-config, key: MEMBER_DB_URL}}
            - name: MEMBER_DB_USERNAME
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: MEMBER_DB_USERNAME}}
            - name: MEMBER_DB_PASSWORD
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: MEMBER_DB_PASSWORD}}
            - name: CORE_DB_URL
              valueFrom: {configMapKeyRef: {name: app-runtime-config, key: CORE_DB_URL}}
            - name: CORE_DB_USERNAME
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: CORE_DB_USERNAME}}
            - name: CORE_DB_PASSWORD
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: CORE_DB_PASSWORD}}
            - name: PAYMENT_DB_URL
              valueFrom: {configMapKeyRef: {name: app-runtime-config, key: PAYMENT_DB_URL}}
            - name: PAYMENT_DB_USERNAME
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: PAYMENT_DB_USERNAME}}
            - name: PAYMENT_DB_PASSWORD
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: PAYMENT_DB_PASSWORD}}
            - name: AI_DB_URL
              valueFrom: {configMapKeyRef: {name: app-runtime-config, key: AI_DB_URL}}
            - name: AI_DB_USERNAME
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: AI_DB_USERNAME}}
            - name: AI_DB_PASSWORD
              valueFrom: {secretKeyRef: {name: app-runtime-secrets, key: AI_DB_PASSWORD}}
EOF

log 'Waiting for all database schemas to reset.'
for _ in $(seq 1 180); do
  succeeded="$(kubectl get job "$reset_job" -n "$namespace" -o jsonpath='{.status.succeeded}' 2>/dev/null || true)"
  failed="$(kubectl get job "$reset_job" -n "$namespace" -o jsonpath='{.status.failed}' 2>/dev/null || true)"

  if [[ "$succeeded" == "1" ]]; then
    kubectl logs "job/${reset_job}" -n "$namespace"
    break
  fi

  if [[ -n "$failed" && "$failed" != "0" ]]; then
    kubectl logs "job/${reset_job}" -n "$namespace" >&2 || true
    fail 'Database reset Job failed.'
  fi

  sleep 2
done

[[ "${succeeded:-}" == "1" ]] || fail 'Database reset Job timed out after 6 minutes.'

if [[ "$defer_flyway" == "true" ]]; then
  log 'Database-only reset completed. No service was restarted.'
  log 'Flyway will run after every affected service is restarted or deployed.'
  exit 0
fi

restore_service() {
  local service_index="$1"
  local service="${services[$service_index]}"
  local replicas="${original_replicas[$service_index]}"
  kubectl scale deployment "$service" -n "$namespace" --replicas="$replicas" >/dev/null

  if [[ "$replicas" -gt 0 ]]; then
    log "Starting ${service}; waiting for Flyway and readiness."
    kubectl rollout status deployment/"$service" -n "$namespace" --timeout=900s
  else
    log "Kept ${service} stopped because its previous replica count was 0."
  fi
}

log "Restoring services affected by ${target} and running Flyway."
for service_index in "${!services[@]}"; do
  restore_service "$service_index"
done

log 'Database reset and Flyway startup completed.'
kubectl get deployments -n "$namespace"
