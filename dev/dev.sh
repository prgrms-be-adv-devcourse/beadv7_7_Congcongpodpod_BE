#!/usr/bin/env bash

set -euo pipefail

# 어느 디렉터리에서 호출해도 dev/compose.yaml과 dev/.env를 사용합니다.
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd -- "$script_dir/.." && pwd)"
env_file="$script_dir/.env"
env_example_file="$script_dir/.env.example"
compose_file="$script_dir/compose.yaml"
config_server_config_dir="$project_root/dev/local/config-server"
log_dir="$script_dir/logs"
run_timestamp="$(date '+%Y%m%d-%H%M%S')"
dev_command="${LASTDISH_DEV_COMMAND:-./dev/dev.sh}"
progress_ui_active=0
progress_ui_height=2
progress_step_lines=()
progress_step_index=0
progress_execution_index=0
current_progress_line=""
completed_progress_percent=0

compose() {
  command docker compose \
    --env-file "$env_file" \
    --project-directory "$script_dir" \
    --file "$script_dir/compose.yaml" \
    "$@"
}

set_progress_bar() {
  local percent="$1"
  local suffix="${2:-}"
  local width=40
  local filled=$((percent * width / 100))
  local empty=$((width - filled))
  local bar=""
  local i

  for ((i = 0; i < filled; i++)); do bar+="█"; done
  for ((i = 0; i < empty; i++)); do bar+="░"; done
  current_progress_line="[$bar] $(printf '%3d' "$percent")%${suffix:+ $suffix}"
}

configure_progress_ui() {
  local label component existing_line merge_start visible_count=0 physical_count=0
  progress_step_lines=()
  for label in "$@"; do
    merge_start=false
    if [[ "$label" == *" 시작" || "$label" == *"재시작" || "$label" == *"기동" || "$label" == *" 실행" ]]; then
      case "$label" in
        *" 시작") component="${label% 시작}" ;;
        *"재시작") component="${label% 재시작}" ;;
        *"기동") component="${label% 기동}" ;;
        *" 실행") component="${label% 실행}" ;;
      esac
      if [[ "$physical_count" -gt 0 ]]; then
        for existing_line in "${progress_step_lines[@]}"; do
          if [[ "$existing_line" == *"] $component 이미지 "* ]]; then
            merge_start=true
            break
          fi
        done
      fi
    fi
    if [[ "$merge_start" == true ]]; then
      progress_step_lines+=("")
    else
      progress_step_lines+=("[대기] $label")
      visible_count=$((visible_count + 1))
    fi
    physical_count=$((physical_count + 1))
  done
  progress_ui_height=$((visible_count + 1))
  progress_step_index=0
  progress_execution_index=0
  completed_progress_percent=0
}

print_progress() {
  local status="$1"
  local percent="$2"
  local label="$3"
  if [[ "$status" == "진행" ]]; then
    set_progress_bar "$percent"
    progress_step_lines[progress_step_index]="[진행] $label"
  else
    set_progress_bar "$percent" "$status"
  fi
  if [[ -t 1 ]]; then
    if [[ "$progress_ui_active" -eq 0 ]]; then
      local row
      for ((row = 0; row < progress_ui_height; row++)); do printf '\n'; done
      printf '\033[%dA\r' "$progress_ui_height"
      progress_ui_active=1
    fi
    render_progress_ui
    if [[ "$status" != "진행" ]]; then close_progress_ui; fi
  else
    if [[ "$status" == "진행" ]]; then
      printf '%s %-4s %s\n' "$current_progress_line" "$status" "$label"
    else
      printf '%s\n' "$current_progress_line"
    fi
  fi
}

render_progress_ui() {
  local step_count=${#progress_step_lines[@]}
  local row step_index=0

  for ((row = 0; row < progress_ui_height; row++)); do
    printf '\r\033[2K'
    while ((step_index < step_count)) && [[ -z "${progress_step_lines[step_index]}" ]]; do
      step_index=$((step_index + 1))
    done
    if ((row < progress_ui_height - 1 && step_index < step_count)); then
      printf '%s' "${progress_step_lines[step_index]}"
      step_index=$((step_index + 1))
    elif ((row == progress_ui_height - 1)); then
      printf '%s' "$current_progress_line"
    fi
    printf '\n'
  done
  printf '\033[%dA\r' "$progress_ui_height"
}

close_progress_ui() {
  if [[ "$progress_ui_active" -eq 1 && -t 1 ]]; then
    printf '\033[%dB\r' "$progress_ui_height"
    progress_ui_active=0
  fi
}

cleanup_progress_ui() {
  close_progress_ui
}

trap cleanup_progress_ui EXIT INT TERM

print_final_summary() {
  local summaries=("$@")
  local summary_count=$#
  local old_height="$progress_ui_height"
  local row

  set_progress_bar 100 "완료"
  if [[ -t 1 && "$progress_ui_active" -eq 1 ]]; then
    for ((row = 0; row < old_height; row++)); do
      printf '\r\033[2K'
      if ((row < summary_count)); then
        printf '[완료] %s' "${summaries[row]}"
      elif ((row == summary_count)); then
        printf '%s' "$current_progress_line"
      fi
      printf '\n'
    done
    if ((old_height > summary_count + 1)); then
      printf '\033[%dA\r' "$((old_height - summary_count - 1))"
    fi
    progress_ui_active=0
  else
    for ((row = 0; row < summary_count; row++)); do
      printf '[완료] %s\n' "${summaries[row]}"
    done
    printf '%s\n' "$current_progress_line"
  fi
}

finish_step() {
  local status="$1"
  local label="$2"
  if [[ -t 1 ]]; then
    progress_step_lines[progress_step_index]="[$status] $label"
    render_progress_ui
  else
    printf '[%s] %s\n' "$status" "$label"
  fi
}

running_step_label() {
  local label="$1"
  case "$label" in
    *"이미지 준비") printf '%s\n' "${label% 준비} 확인·다운로드 중" ;;
    *"이미지 빌드") printf '%s\n' "$label 중" ;;
    *"서비스 시작 및 Flyway 적용") printf '%s\n' "전체 서비스 시작 및 Flyway 적용 중" ;;
    *"이미지 빌드 및 서비스 시작") printf '%s\n' "이미지 빌드 및 서비스 시작 중" ;;
    *"재시작") printf '%s\n' "$label 중" ;;
    *"기동") printf '%s\n' "$label 중" ;;
    *) printf '%s\n' "$label 중" ;;
  esac
}

completed_step_label() {
  local label="$1"
  case "$label" in
    *"이미지 준비") printf '%s\n' "${label% 준비} 준비됨" ;;
    *"이미지 빌드") printf '%s\n' "${label% 빌드} 빌드됨" ;;
    *"서비스 시작 및 Flyway 적용") printf '%s\n' "전체 서비스 시작됨 · Flyway 적용됨" ;;
    *"이미지 빌드 및 서비스 시작") printf '%s\n' "이미지 빌드됨 · 서비스 시작됨" ;;
    *"컨테이너 제거") printf '%s\n' "${label% 제거} 제거됨" ;;
    *"볼륨 제거") printf '%s\n' "${label% 제거} 제거됨" ;;
    *"네트워크 제거") printf '%s\n' "${label% 제거} 제거됨" ;;
    *"서비스 중지"|*"Service 중지"|*"컨테이너 중지") printf '%s\n' "${label% 중지} 중지됨" ;;
    *"재시작") printf '%s\n' "${label% 재시작} 재시작됨" ;;
    *"기동") printf '%s\n' "${label% 기동} 시작됨" ;;
    *" 시작") printf '%s\n' "${label% 시작} 시작됨" ;;
    *" 실행") printf '%s\n' "${label% 실행} 실행됨" ;;
    *"Spotless 검사 및 자동 수정") printf '%s\n' "Spotless 검사 및 자동 수정됨" ;;
    *) printf '%s\n' "$label" ;;
  esac
}

run_step() {
  local percent="$1"
  local label="$2"
  shift 2

  local log_file
  local command_pid command_status=0
  local spinner_index=0
  local running_label completed_label
  local planned_step_index component candidate_index
  local spinner_frames=("⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏")
  running_label="$(running_step_label "$label")"
  completed_label="$(completed_step_label "$label")"
  planned_step_index="$progress_execution_index"
  progress_execution_index=$((progress_execution_index + 1))
  progress_step_index="$planned_step_index"
  if [[ "$label" == *" 시작" || "$label" == *"재시작" || "$label" == *"기동" || "$label" == *" 실행" ]]; then
    case "$label" in
      *" 시작") component="${label% 시작}" ;;
      *"재시작") component="${label% 재시작}" ;;
      *"기동") component="${label% 기동}" ;;
      *" 실행") component="${label% 실행}" ;;
    esac
    for ((candidate_index = 0; candidate_index < planned_step_index; candidate_index++)); do
      if [[ "${progress_step_lines[candidate_index]}" == *"] $component 이미지 "* ]]; then
        progress_step_index="$candidate_index"
        break
      fi
    done
  fi
  log_file="$(mktemp)"
  print_progress "진행" "$completed_progress_percent" "$running_label"

  "$@" >"$log_file" 2>&1 &
  command_pid=$!
  if [[ -t 1 ]]; then
    while kill -0 "$command_pid" >/dev/null 2>&1; do
      progress_step_lines[progress_step_index]="[${spinner_frames[spinner_index]}] $running_label"
      render_progress_ui
      spinner_index=$(((spinner_index + 1) % ${#spinner_frames[@]}))
      sleep 0.12
    done
    progress_step_lines[progress_step_index]="[진행] $running_label"
  fi

  if wait "$command_pid"; then
    rm -f "$log_file"
    completed_progress_percent="$percent"
    set_progress_bar "$completed_progress_percent"
    finish_step "완료" "$completed_label"
    return 0
  else
    command_status=$?
    local error_file="$log_dir/[ERROR]-$run_timestamp.log"
    local raw_error_file
    raw_error_file="$(mktemp)"
    mkdir -p "$log_dir"
    {
      printf '실행 시각: %s\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')"
      printf '실패 단계: %s\n' "$label"
      printf '종료 코드: %d\n' "$command_status"
      printf '실행 명령:'
      printf ' %q' "$@"
      printf '\n\n'
      cat "$log_file"
    } >"$raw_error_file"
    # macOS·Git Bash·WSL에서 동일하게 BOM 없는 UTF-8 로그를 생성합니다.
    iconv -c -f UTF-8 -t UTF-8 "$raw_error_file" >"$error_file"
    rm -f "$log_file" "$raw_error_file"
    finish_step "오류" "$label" >&2
    close_progress_ui
    printf '상세 로그: "%s"\n' "$error_file" >&2
    return "$command_status"
  fi
}

remove_volume_if_exists() {
  local volume="$1"
  if docker volume inspect "$volume" >/dev/null 2>&1; then
    docker volume rm "$volume" >/dev/null
  fi
}

prepare_postgresql_image() {
  compose pull --policy missing member-db core-db database-initializer
}

prepare_service_image() {
  compose pull --policy missing "$1"
}

run_spotless_if_available() {
  if ! grep -RqsE "com[.]diffplug[.]spotless|id ['\"]com[.]diffplug[.]spotless['\"]" \
    "$project_root/backend" --include='*.gradle' --include='*.gradle.kts'; then
    return 0
  fi

  # 로컬 up에서는 Gradle daemon을 재사용해 매 실행마다 JVM을 다시 띄우는 비용을 줄입니다.
  if (cd "$project_root/backend" && ./gradlew spotlessCheck); then
    return 0
  fi
  (cd "$project_root/backend" && ./gradlew spotlessApply)
  (cd "$project_root/backend" && ./gradlew spotlessCheck)
}

start_database_and_wait() {
  compose up -d --wait --wait-timeout 120 "$1"
}

start_core_database_with_initializer() {
  compose up -d --wait --wait-timeout 120 core-db
  compose up -d database-initializer
  compose wait database-initializer
}

service_named_volumes() {
  local target="$1"
  awk -v target="$target" '
    $0 == "  " target ":" { in_service=1; next }
    in_service && /^  [A-Za-z0-9_-]+:$/ { exit }
    in_service && /^    volumes:$/ { in_volumes=1; next }
    in_volumes && /^    [A-Za-z0-9_-]+:$/ { in_volumes=0 }
    in_volumes && /^      - [A-Za-z0-9_-]+:/ {
      volume=$0
      sub(/^      - /, "", volume)
      sub(/:.*/, "", volume)
      print volume
    }
    in_volumes && /^        source: [A-Za-z0-9_-]+$/ {
      volume=$0
      sub(/^        source: /, "", volume)
      print volume
    }
  ' "$compose_file"
}

remove_service_volumes() {
  local project_name target volume
  project_name="$(awk -F': *' '/^name:/{print $2; exit}' "$compose_file")"
  for target in "$@"; do
    while IFS= read -r volume; do
      [[ -z "$volume" ]] && continue
      remove_volume_if_exists "${project_name}_${volume}"
    done < <(service_named_volumes "$target")
  done
}

env_value() {
  local key="$1"
  awk -v key="$key" '
    index($0, key "=") == 1 {
      print substr($0, length(key) + 2)
      exit
    }
  ' "$env_file"
}

compose_services() {
  awk '
    /^services:$/ { in_services=1; next }
    in_services && /^[^[:space:]]/ { exit }
    in_services && /^  [A-Za-z0-9_-]+:$/ {
      name=$0
      sub(/^  /, "", name)
      sub(/:$/, "", name)
      print name
    }
  ' "$compose_file"
}

service_display_name() {
  case "$1" in
    member-db) printf 'Member DB\n' ;;
    core-db) printf 'Core DB\n' ;;
    database-initializer) printf 'Database Initializer\n' ;;
    redis) printf 'Redis\n' ;;
    kafka) printf 'Kafka\n' ;;
    elasticsearch) printf 'Elasticsearch\n' ;;
    config-server) printf 'Config Server\n' ;;
    member-service) printf 'Member Service\n' ;;
    core-service) printf 'Core Service\n' ;;
    payment-service) printf 'Payment Service\n' ;;
    ai-service) printf 'AI Service\n' ;;
    gateway-service) printf 'Gateway Service\n' ;;
    *) printf '%s\n' "$1" ;;
  esac
}

validate_services() {
  [[ $# -eq 0 ]] && return 0

  local available_services target
  available_services="$(compose_services)"
  for target in "$@"; do
    if ! grep -Fqx "$target" <<<"$available_services"; then
      echo "알 수 없는 서비스입니다: $target" >&2
      echo "$dev_command help up에서 Compose 서비스 목록을 확인하세요." >&2
      return 2
    fi
  done
}

compose_env_keys() {
  # $$로 escape한 컨테이너 내부 변수는 제외하고 Compose 보간 변수만 추출합니다.
  sed -E 's/\$\$\{[^}]+\}//g' "$compose_file" \
    | grep -oE '\$\{[A-Z0-9_]+[^}]*\}' \
    | sed -E 's/^\$\{([A-Z0-9_]+).*$/\1/' \
    | sort -u
}

validate_env() {
  if [[ ! -f "$env_file" ]]; then
    echo "[환경변수 오류] dev/.env 파일이 없습니다." >&2
    echo "생성: cp dev/.env.example dev/.env" >&2
    return 2
  fi
  if [[ ! -f "$env_example_file" ]]; then
    echo "[환경변수 오류] 기준 파일 dev/.env.example이 없습니다." >&2
    return 2
  fi

  local errors=()
  local key count value
  while IFS= read -r key; do
    if ! grep -Eq "^${key}=" "$env_example_file"; then
      errors+=("dev/.env.example에 없는 Compose 환경변수: $key")
    fi
  done < <(compose_env_keys)

  while IFS= read -r key; do
    count="$(grep -Ec "^${key}=" "$env_file" || true)"
    if [[ "$count" -eq 0 ]]; then
      errors+=("누락된 키: $key")
    elif [[ "$count" -gt 1 ]]; then
      errors+=("중복된 키: $key")
    else
      case "$key" in
        S3_BUCKET|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|AWS_SESSION_TOKEN|S3_ENDPOINT) ;;
        *)
          value="$(env_value "$key")"
          if [[ -z "${value//[[:space:]]/}" ]]; then
            errors+=("값이 비어 있음: $key")
          fi
          ;;
      esac
    fi
  done < <(awk -F= '/^[A-Za-z_][A-Za-z0-9_]*=/{print $1}' "$env_example_file")

  if [[ "$(grep -Ec '^S3_ENABLED=' "$env_file" || true)" -eq 1 ]]; then
    value="$(env_value S3_ENABLED)"
    if [[ "$value" != "true" && "$value" != "false" ]]; then
      errors+=("S3_ENABLED는 true 또는 false여야 함: $value")
    elif [[ "$value" == "true" ]]; then
      for key in S3_BUCKET AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY; do
        if [[ "$(grep -Ec "^${key}=" "$env_file" || true)" -eq 1 ]] &&
           [[ -z "$(env_value "$key" | tr -d '[:space:]')" ]]; then
          errors+=("S3_ENABLED=true일 때 필수: $key")
        fi
      done
    fi
  fi

  if [[ ${#errors[@]} -gt 0 ]]; then
    echo "[환경변수 오류] dev/.env를 확인하세요:" >&2
    printf '  - %s\n' "${errors[@]}" >&2
    return 2
  fi
}

validate_config_server_files() {
  local required_files=(
    application.yml
    member-service.yml
    core-service.yml
    payment-service.yml
    ai-service.yml
    gateway-service.yml
  )
  local file
  local missing_files=()

  for file in "${required_files[@]}"; do
    if [[ ! -f "$config_server_config_dir/$file" ]]; then
      missing_files+=("$file")
    fi
  done

  if [[ ${#missing_files[@]} -gt 0 ]]; then
    echo "[Config Server 오류] 필수 설정 파일이 없습니다: $config_server_config_dir" >&2
    printf '  - %s\n' "${missing_files[@]}" >&2
    return 2
  fi
}

print_help() {
  case "${1:-}" in
    "")
      cat <<EOF
LastDish 로컬 개발환경 관리

명령:
  up      서비스 빌드 및 시작
  stop    서비스 중지
  ps      실행 중인 서비스 상태 조회
  logs    서비스 로그 조회
  down    서비스 컨테이너 제거
  reset   로컬 데이터 초기화
  install 프로젝트 로컬 git dev 명령 등록
  uninstall 프로젝트 로컬 git dev 명령 제거

상세 도움말:
  $dev_command help up
  $dev_command help stop
  $dev_command help ps
  $dev_command help logs
  $dev_command help down
  $dev_command help reset
  $dev_command help install
EOF
      ;;
    ps)
      cat <<EOF
사용법: $dev_command ps [all|서비스 ...]

동작:
  - 서비스가 없으면 현재 실행 중인 모든 Compose 서비스 상태를 표시합니다.
  - all은 서비스 인자를 생략한 것과 동일합니다.
  - 서비스를 지정하면 해당 서비스의 상태만 표시합니다.
  - 서비스명, 상태, 실행 시간·헬스 상태, 공개 포트를 간결히 표시합니다.
  - 컨테이너 상태를 변경하지 않습니다.

예시:
  $dev_command ps
  $dev_command ps all
  $dev_command ps core-service
  $dev_command ps member-service payment-service
EOF
      ;;
    install|uninstall)
      cat <<EOF
사용법:
  $dev_command install
  $dev_command uninstall

동작:
  install
    현재 저장소의 로컬 Git alias에 git dev를 등록합니다.
    등록 후 프로젝트 내부 어느 디렉터리에서든 git dev <명령>을 사용할 수 있습니다.
  uninstall
    현재 저장소에 등록된 git dev alias만 제거합니다.

예시:
  git dev up
  git dev ps
  git dev logs core-service
  git dev reset all
EOF
      ;;
    up)
      cat <<EOF
사용법: $dev_command up [all|서비스 ...]

동작:
  - 서비스를 생략하면 DB·메시징·검색·애플리케이션을 포함한 전체 환경을 시작합니다.
  - all은 서비스 인자를 생략한 것과 동일합니다.
  - 서비스를 지정하면 해당 이미지를 빌드하고 Compose depends_on에 선언된 의존 서비스도 함께 시작합니다.
  - 변경 없는 이미지 레이어는 Docker 빌드 캐시를 사용하고, 변경된 서비스는 새 이미지로 컨테이너를 교체합니다.
  - 실행 결과는 백그라운드(-d)로 유지됩니다.
  - 성공 시 진행 단계만 표시하고, 실패 상세는 dev/logs의 [ERROR] 로그 파일에 저장합니다.

주요 의존 관계:
  member-service   -> config-server, member-db, redis, kafka
  core-service     -> config-server, core-db, redis, kafka
  payment-service  -> config-server, core-db, database-initializer, redis, kafka
  ai-service       -> config-server, core-db, database-initializer, redis, kafka, elasticsearch
  gateway-service  -> config-server, redis 및 백엔드 애플리케이션 서비스

참고:
  Spotless plugin이 있으면 빌드 전에 spotlessCheck를 실행합니다.
  검사 실패 시 spotlessApply로 자동 수정한 뒤 spotlessCheck를 다시 실행합니다.
  up은 데이터를 초기화하지 않습니다. 기존 볼륨의 DB·Kafka·Elasticsearch 데이터가 유지됩니다.
  서비스명부터 입력하는 축약 방식도 지원합니다.

예시:
  $dev_command up
  $dev_command up all
  $dev_command core-service gateway-service
EOF
      ;;
    stop)
      cat <<EOF
사용법: $dev_command stop [서비스 ...]

동작:
  - 서비스를 지정하면 해당 컨테이너만 중지합니다.
  - 지정한 서비스의 의존 컨테이너와 다른 서비스는 계속 실행됩니다.
  - 서비스를 생략하면 Compose 환경의 전체 컨테이너를 중지합니다.
  - 컨테이너는 삭제하지 않으므로 다음 up에서 기존 컨테이너를 다시 사용할 수 있습니다.

보존 항목:
  컨테이너 설정, 이미지, 데이터 볼륨, 공용 네트워크

예시:
  $dev_command stop core-service
  $dev_command stop core-service payment-service
  $dev_command stop
EOF
      ;;
    logs|log)
      cat <<EOF
사용법: $dev_command logs [-line 줄수] [서비스 ...]

동작:
  - 서비스가 없거나 all이면 실행 중인 전체 Compose 컨테이너 로그를 표시합니다.
  - 여러 서비스명을 입력하면 로그를 시간순으로 섞어 함께 표시합니다.
  - 기본적으로 각 서비스의 최근 200줄부터 시작해 새 로그를 계속 추적합니다.
  - -line 옵션으로 시작 줄 수를 변경할 수 있으며 서비스명 앞뒤 어디에 두어도 됩니다.
  - 로그 조회는 컨테이너 상태를 변경하지 않습니다. Ctrl+C로 조회만 종료합니다.

예시:
  $dev_command logs
  $dev_command logs all
  $dev_command logs core-service payment-service
  $dev_command logs -line 30 core-service
  $dev_command logs redis kafka elasticsearch -line 50
EOF
      ;;
    down)
      cat <<EOF
사용법: $dev_command down [-v|--volumes] [서비스 ...]

서비스 지정 시:
  - 지정한 컨테이너만 중지한 뒤 제거합니다.
  - 의존 컨테이너, 다른 서비스, 공용 네트워크는 유지합니다.
  - 다음 up에서 제거된 컨테이너를 다시 생성합니다.
  - -v를 지정하면 해당 서비스에 연결된 명명 볼륨도 삭제합니다.

서비스 생략 시:
  - Compose 환경의 전체 컨테이너와 공용 네트워크를 제거합니다.
  - 다음 up에서 전체 컨테이너와 네트워크를 다시 생성합니다.
  - -v를 지정하면 Compose 환경의 명명 볼륨도 모두 삭제합니다.

항상 보존되는 항목:
  서비스 이미지

주의:
  -v/--volumes로 삭제한 데이터는 복구할 수 없습니다.
  옵션이 없으면 DB·Kafka·Elasticsearch 데이터 볼륨을 보존합니다.

예시:
  $dev_command down core-service
  $dev_command down core-service payment-service
  $dev_command down
  $dev_command down -v
  $dev_command down member-db -v
EOF
      ;;
    reset)
      cat <<EOF
사용법: $dev_command reset <대상>

공통 동작:
  - 별도 확인 없이 지정한 대상 데이터를 즉시 삭제합니다.
  - 데이터만 초기화하고 애플리케이션 이미지는 빌드하거나 시작하지 않습니다.
  - 이후 up으로 애플리케이션을 시작할 때 최신 Flyway migration과 seed가 적용됩니다.
  - 성공한 명령의 상세 출력은 숨기고, 실패 로그는 dev/logs/[ERROR]-날짜시간.log에 저장합니다.

대상:
  member-db
    Member Service를 중지하고 member_db만 삭제·재생성합니다.
  core-db
    Core Service를 중지하고 core_db만 삭제·재생성합니다.
  payment-db
    Payment Service를 중지하고 payment_db만 삭제·재생성합니다.
  ai-db
    AI Service를 중지하고 ai_db만 삭제·재생성합니다.
  redis
    Redis의 전체 키를 FLUSHALL로 삭제하며 컨테이너는 유지합니다.
  kafka
    Kafka 컨테이너와 데이터 볼륨을 제거한 뒤 새로 생성합니다.
  elasticsearch
    Elasticsearch 컨테이너와 데이터 볼륨을 제거한 뒤 새로 생성합니다.
  all
    전체 컨테이너를 제거하고 DB·Kafka·Elasticsearch 데이터를 삭제합니다.
    데이터 인프라 컨테이너만 생성하며 시작하지 않습니다.
    이후 $dev_command up 실행 시 전체 서비스가 시작되고 Flyway가 적용됩니다.

주의:
  초기화한 데이터는 복구할 수 없습니다.
  개별 DB 초기화는 다른 논리 DB와 Redis·Kafka·Elasticsearch 데이터를 유지합니다.

예시:
  $dev_command reset core-db
  $dev_command reset all
EOF
      ;;
    *)
      echo "도움말을 찾을 수 없는 명령입니다: $1" >&2
      echo "사용 가능: up, stop, ps, logs, down, reset, install, uninstall" >&2
      return 2
      ;;
  esac

  case "${1:-}" in
    up|stop|ps|logs|log|down)
      echo
      echo "현재 Compose 서비스:"
      compose_services | sed 's/^/  /'
      ;;
  esac
}

cd "$project_root"

if [[ $# -eq 0 ]]; then
  echo "명령을 입력해야 합니다. 전체 시작: $dev_command up" >&2
  echo "도움말: $dev_command help" >&2
  exit 2
fi
command="$1"
shift

# 도움말 외 모든 명령은 Docker 호출 전에 환경변수 전체를 검증합니다.
case "$command" in
  -h|--help|help) ;;
  install|uninstall) ;;
  *) validate_env ;;
esac

# up/ps의 all은 대상 인자를 생략한 것과 동일합니다. stop/down은 기존 동작을 유지합니다.
case "$command" in
  up|ps)
    if [[ $# -eq 1 && "$1" == "all" ]]; then
      set --
    elif [[ " $* " == *" all "* ]]; then
      echo "all은 다른 서비스명과 함께 사용할 수 없습니다." >&2
      exit 2
    fi
    ;;
esac

case "$command" in
  install)
    dev_alias="!LASTDISH_DEV_COMMAND='git dev' ./dev/dev.sh"
    legacy_dev_alias='!./dev/dev.sh'
    existing_alias="$(git config --local --get alias.dev 2>/dev/null || true)"
    if [[ "$existing_alias" == "$dev_alias" ]]; then
      echo "[안내] git dev 명령이 이미 현재 프로젝트에 등록되어 있습니다."
      exit 0
    elif [[ -n "$existing_alias" && "$existing_alias" != "$legacy_dev_alias" ]]; then
      echo "[오류] 다른 git dev alias가 이미 등록되어 있어 덮어쓰지 않았습니다." >&2
      echo "현재 값: $existing_alias" >&2
      exit 2
    fi
    git config --local alias.dev "$dev_alias"
    echo "[완료] git dev 명령을 현재 프로젝트에 등록했습니다."
    echo "사용 예: git dev ps"
    exit 0
    ;;
  uninstall)
    dev_alias="!LASTDISH_DEV_COMMAND='git dev' ./dev/dev.sh"
    legacy_dev_alias='!./dev/dev.sh'
    existing_alias="$(git config --local --get alias.dev 2>/dev/null || true)"
    if [[ -z "$existing_alias" ]]; then
      echo "[안내] git dev 명령은 이미 제거되어 있습니다."
      exit 0
    elif [[ "$existing_alias" != "$dev_alias" && "$existing_alias" != "$legacy_dev_alias" ]]; then
      echo "[오류] git dev가 다른 alias로 등록되어 있어 제거하지 않았습니다." >&2
      echo "현재 값: $existing_alias" >&2
      exit 2
    fi
    git config --local --unset-all alias.dev
    echo "[완료] 현재 프로젝트의 git dev 명령을 제거했습니다."
    exit 0
    ;;
  down)
    remove_volumes=false
    down_services=()
    down_service_count=0
    while [[ $# -gt 0 ]]; do
      case "$1" in
        -v|--volumes) remove_volumes=true ;;
        -*)
          echo "알 수 없는 down 옵션입니다: $1" >&2
          echo "사용 가능: -v, --volumes" >&2
          exit 2
          ;;
        *)
          down_services+=("$1")
          down_service_count=$((down_service_count + 1))
          ;;
      esac
      shift
    done
    if [[ "$down_service_count" -gt 0 ]]; then
      set -- "${down_services[@]}"
    else
      set --
    fi

    # 서비스 지정 시 해당 컨테이너만 제거하고, 생략 시 전체 컨테이너와 네트워크를 제거합니다.
    if [[ $# -eq 0 ]]; then
      down_services=()
      down_labels=()
      down_service_count=0
      while IFS= read -r target; do
        down_services+=("$target")
        down_labels+=("$(service_display_name "$target") 컨테이너 제거")
        down_service_count=$((down_service_count + 1))
      done < <(compose_services)
      if [[ "$remove_volumes" == true ]]; then
        down_labels+=("네트워크·볼륨 제거")
      else
        down_labels+=("네트워크 제거")
      fi
      configure_progress_ui "${down_labels[@]}"

      down_step=0
      down_total=$((down_service_count + 1))
      for target in "${down_services[@]}"; do
        down_step=$((down_step + 1))
        run_step $((down_step * 100 / down_total)) "$(service_display_name "$target") 컨테이너 제거" \
          compose rm --stop --force "$target"
      done
      if [[ "$remove_volumes" == true ]]; then
        run_step 100 "네트워크·볼륨 제거" compose down --volumes
      else
        run_step 100 "네트워크 제거" compose down
      fi
      print_progress "완료" 100 "전체 환경 제거"
    else
      available_services="$(compose_services)"
      for target in "$@"; do
        if ! grep -Fqx "$target" <<<"$available_services"; then
          echo "알 수 없는 서비스입니다: $target" >&2
          echo "$dev_command help에서 Compose 서비스 목록을 확인하세요." >&2
          exit 2
        fi
      done

      down_labels=()
      for target in "$@"; do down_labels+=("$(service_display_name "$target") 컨테이너 제거"); done
      if [[ "$remove_volumes" == true ]]; then
        for target in "$@"; do down_labels+=("$(service_display_name "$target") 명명 볼륨 제거"); done
      fi
      configure_progress_ui "${down_labels[@]}"

      down_step=0
      down_total=${#down_labels[@]}
      for target in "$@"; do
        down_step=$((down_step + 1))
        run_step $((down_step * 100 / down_total)) "$(service_display_name "$target") 컨테이너 제거" \
          compose rm --stop --force "$target"
      done
      if [[ "$remove_volumes" == true ]]; then
        for target in "$@"; do
          down_step=$((down_step + 1))
          run_step $((down_step * 100 / down_total)) "$(service_display_name "$target") 명명 볼륨 제거" \
            remove_service_volumes "$target"
        done
      fi
      print_progress "완료" 100 "서비스 제거"
    fi
    exit 0
    ;;
  stop)
    # 지정한 서비스를 각각 중지합니다. 서비스명이 없으면 전체 서비스를 순차 중지합니다.
    stop_services=()
    stop_labels=()
    stop_service_count=0
    if [[ $# -eq 0 ]]; then
      while IFS= read -r target; do
        stop_services+=("$target")
        stop_labels+=("$(service_display_name "$target") 컨테이너 중지")
        stop_service_count=$((stop_service_count + 1))
      done < <(compose_services)
    else
      available_services="$(compose_services)"
      for target in "$@"; do
        if ! grep -Fqx "$target" <<<"$available_services"; then
          echo "알 수 없는 서비스입니다: $target" >&2
          echo "$dev_command help stop에서 Compose 서비스 목록을 확인하세요." >&2
          exit 2
        fi
        stop_services+=("$target")
        stop_labels+=("$(service_display_name "$target") 컨테이너 중지")
        stop_service_count=$((stop_service_count + 1))
      done
    fi

    configure_progress_ui "${stop_labels[@]}"
    stop_step=0
    for target in "${stop_services[@]}"; do
      stop_step=$((stop_step + 1))
      run_step $((stop_step * 100 / stop_service_count)) "$(service_display_name "$target") 컨테이너 중지" \
        compose stop "$target"
    done
    print_progress "완료" 100 "서비스 중지"
    exit 0
    ;;
  ps)
    if [[ $# -gt 0 ]]; then
      available_services="$(compose_services)"
      for target in "$@"; do
        if ! grep -Fqx "$target" <<<"$available_services"; then
          echo "알 수 없는 서비스입니다: $target" >&2
          echo "$dev_command help ps에서 Compose 서비스 목록을 확인하세요." >&2
          exit 2
        fi
      done
    fi
    compose ps --format 'table {{.Service}}\t{{.State}}\t{{.Status}}\t{{.Ports}}' "$@" \
      | sed -E 's/, \[::\]:[^,[:space:]]+//g'
    exit 0
    ;;
  log|logs)
    tail_lines="${LOG_TAIL:-200}"
    services=()
    while [[ $# -gt 0 ]]; do
      case "$1" in
        -line)
          if [[ $# -lt 2 || ! "$2" =~ ^[1-9][0-9]*$ ]]; then
            echo "-line 뒤에는 1 이상의 정수를 입력하세요." >&2
            exit 2
          fi
          tail_lines="$2"
          shift 2
          ;;
        *)
          services+=("$1")
          shift
          ;;
      esac
    done
    if [[ ${#services[@]} -eq 1 && "${services[0]}" == "all" ]]; then
      services=()
    elif [[ ${#services[@]} -gt 0 ]]; then
      available_services="$(compose_services)"
      for target in "${services[@]}"; do
        if ! grep -Fqx "$target" <<<"$available_services"; then
          echo "알 수 없는 서비스입니다: $target" >&2
          echo "$dev_command help에서 Compose 서비스 목록을 확인하세요." >&2
          exit 2
        fi
      done
    fi
    if [[ ${#services[@]} -eq 0 ]]; then
      echo "[로그] 전체 컨테이너 (종료: Ctrl+C)"
    else
      echo "[로그] ${services[*]} (종료: Ctrl+C)"
    fi
    if [[ ${#services[@]} -eq 0 ]]; then
      compose logs --follow --tail="$tail_lines"
    else
      compose logs --follow --tail="$tail_lines" "${services[@]}"
    fi
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

    case "$target" in
      member-db) configure_progress_ui "Member DB 이미지 준비" "Gateway Service 중지" "Member Service 중지" "Member DB 기동" "member_db 재생성" ;;
      core-db) configure_progress_ui "Core DB 이미지 준비" "Gateway Service 중지" "Core Service 중지" "Payment Service 중지" "AI Service 중지" "Core DB 기동" "core_db 재생성" ;;
      payment-db) configure_progress_ui "Core DB 이미지 준비" "Gateway Service 중지" "Payment Service 중지" "Core DB 기동" "payment_db 재생성" ;;
      ai-db) configure_progress_ui "Core DB 이미지 준비" "Gateway Service 중지" "AI Service 중지" "Core DB 기동" "ai_db 재생성" ;;
      kafka) configure_progress_ui "Kafka 이미지 준비" "Gateway Service 중지" "Member Service 중지" "Core Service 중지" "Payment Service 중지" "AI Service 중지" "Kafka 중지" "Kafka 컨테이너 제거" "Kafka 데이터 볼륨 제거" "Kafka 재시작" ;;
      redis) configure_progress_ui "Redis 이미지 준비" "Gateway Service 중지" "Member Service 중지" "Core Service 중지" "Payment Service 중지" "AI Service 중지" "Redis 기동" "Redis 데이터 초기화" ;;
      elasticsearch) configure_progress_ui "Elasticsearch 이미지 준비" "Gateway Service 중지" "AI Service 중지" "Elasticsearch 중지" "Elasticsearch 컨테이너 제거" "Elasticsearch 데이터 볼륨 제거" "Elasticsearch 재시작" ;;
      all) configure_progress_ui "PostgreSQL 이미지 준비" "Redis 이미지 준비" "Kafka 이미지 준비" "Elasticsearch 이미지 준비" "전체 컨테이너 제거" "Member DB 데이터 볼륨 제거" "Core DB 데이터 볼륨 제거" "Kafka 데이터 볼륨 제거" "Elasticsearch 데이터 볼륨 제거" "데이터 인프라 컨테이너 생성" "애플리케이션 컨테이너 미실행 상태 확인" ;;
    esac

    case "$target" in
      member-db)
        run_step 10 "Member DB 이미지 준비" prepare_postgresql_image
        run_step 20 "Gateway Service 중지" compose stop gateway-service
        run_step 35 "Member Service 중지" compose stop member-service
        run_step 60 "Member DB 기동" start_database_and_wait member-db
        run_step 90 "member_db 재생성" compose exec -T member-db psql -v ON_ERROR_STOP=1 -U member -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'member_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS member_db;" \
          -c "CREATE DATABASE member_db OWNER member;"
        ;;
      core-db)
        run_step 10 "Core DB 이미지 준비" prepare_postgresql_image
        run_step 15 "Gateway Service 중지" compose stop gateway-service
        run_step 25 "Core Service 중지" compose stop core-service
        run_step 35 "Payment Service 중지" compose stop payment-service
        run_step 45 "AI Service 중지" compose stop ai-service
        run_step 65 "Core DB 기동" start_database_and_wait core-db
        run_step 90 "core_db 재생성" compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'core_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS core_db;" \
          -c "CREATE DATABASE core_db OWNER core;"
        ;;
      payment-db)
        run_step 10 "Core DB 이미지 준비" prepare_postgresql_image
        run_step 20 "Gateway Service 중지" compose stop gateway-service
        run_step 35 "Payment Service 중지" compose stop payment-service
        run_step 60 "Core DB 기동" start_core_database_with_initializer
        run_step 90 "payment_db 재생성" compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'payment_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS payment_db;" \
          -c "CREATE DATABASE payment_db OWNER payment;"
        ;;
      ai-db)
        run_step 10 "Core DB 이미지 준비" prepare_postgresql_image
        run_step 20 "Gateway Service 중지" compose stop gateway-service
        run_step 35 "AI Service 중지" compose stop ai-service
        run_step 60 "Core DB 기동" start_core_database_with_initializer
        run_step 90 "ai_db 재생성" compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres \
          -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'ai_db' AND pid <> pg_backend_pid();" \
          -c "DROP DATABASE IF EXISTS ai_db;" \
          -c "CREATE DATABASE ai_db OWNER ai;"
        ;;
      kafka)
        run_step 5 "Kafka 이미지 준비" prepare_service_image kafka
        run_step 10 "Gateway Service 중지" compose stop gateway-service
        run_step 15 "Member Service 중지" compose stop member-service
        run_step 20 "Core Service 중지" compose stop core-service
        run_step 25 "Payment Service 중지" compose stop payment-service
        run_step 30 "AI Service 중지" compose stop ai-service
        run_step 40 "Kafka 중지" compose stop kafka
        run_step 55 "Kafka 컨테이너 제거" compose rm -f kafka
        run_step 70 "Kafka 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_kafka-data
        run_step 90 "Kafka 재시작" compose up -d kafka
        ;;
      redis)
        run_step 5 "Redis 이미지 준비" prepare_service_image redis
        run_step 10 "Gateway Service 중지" compose stop gateway-service
        run_step 15 "Member Service 중지" compose stop member-service
        run_step 20 "Core Service 중지" compose stop core-service
        run_step 25 "Payment Service 중지" compose stop payment-service
        run_step 35 "AI Service 중지" compose stop ai-service
        run_step 60 "Redis 기동" compose up -d redis
        run_step 90 "Redis 데이터 초기화" compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli FLUSHALL'
        ;;
      elasticsearch)
        run_step 10 "Elasticsearch 이미지 준비" compose build elasticsearch
        run_step 15 "Gateway Service 중지" compose stop gateway-service
        run_step 25 "AI Service 중지" compose stop ai-service
        run_step 40 "Elasticsearch 중지" compose stop elasticsearch
        run_step 55 "Elasticsearch 컨테이너 제거" compose rm -f elasticsearch
        run_step 70 "Elasticsearch 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_elasticsearch-data
        run_step 90 "Elasticsearch 재시작" compose up -d elasticsearch
        ;;
      all)
        # 전체 컨테이너를 제거한 뒤 모든 영속 데이터 볼륨을 삭제합니다.
        # Redis는 비영속 구성이므로 컨테이너 제거만으로 데이터가 초기화됩니다.
        run_step 5 "PostgreSQL 이미지 준비" prepare_postgresql_image
        run_step 10 "Redis 이미지 준비" prepare_service_image redis
        run_step 15 "Kafka 이미지 준비" prepare_service_image kafka
        run_step 20 "Elasticsearch 이미지 준비" compose build elasticsearch
        run_step 25 "전체 컨테이너 제거" compose down
        run_step 35 "Member DB 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_member-db-data
        run_step 45 "Core DB 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_core-db-data
        run_step 55 "Kafka 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_kafka-data
        run_step 65 "Elasticsearch 데이터 볼륨 제거" remove_volume_if_exists lastdish-local_elasticsearch-data

        run_step 85 "데이터 인프라 컨테이너 생성" compose create \
          member-db core-db redis kafka elasticsearch
        run_step 95 "애플리케이션 컨테이너 미실행 상태 확인" compose ps --status running
        ;;
    esac
    print_progress "완료" 100 "'$target' 초기화"
    exit 0
    ;;
  up)
    ;;
  -h|--help|help)
    print_help "${1:-}"
    exit 0
    ;;
  *)
    # 서비스명만 전달하는 간단한 사용법(./dev/dev.sh member-service)을 지원합니다.
    set -- "$command" "$@"
    ;;
esac

# up 대상은 Docker를 호출하기 전에 검증하여 오타가 빌드나 컨테이너 변경으로 이어지지 않게 합니다.
validate_config_server_files
validate_services "$@"

# 빌드 전 현재 LastDish Compose 컨테이너가 참조하는 이미지 ID를 기록합니다.
# 빌드 실패 시 즉시 종료되므로 기존 이미지는 삭제되지 않습니다.
before_images="$(compose images -q 2>/dev/null | sort -u || true)"
final_summary=()

if [[ $# -eq 0 ]]; then
  final_summary=(
    "Member DB 시작됨"
    "Core DB 시작됨"
    "Database Initializer 실행됨"
    "Redis 시작됨"
    "Kafka 시작됨"
    "Elasticsearch 시작됨"
    "Config Server 시작됨"
    "Member Service 시작됨"
    "Core Service 시작됨"
    "Payment Service 시작됨"
    "AI Service 시작됨"
    "Gateway Service 시작됨"
  )
  configure_progress_ui \
    "Spotless 검사 및 자동 수정" \
    "Member DB 이미지 준비" \
    "Core DB 이미지 준비" \
    "Database Initializer 이미지 준비" \
    "Redis 이미지 준비" \
    "Kafka 이미지 준비" \
    "Elasticsearch 이미지 빌드" \
    "Config Server 이미지 빌드" \
    "Member Service 이미지 빌드" \
    "Core Service 이미지 빌드" \
    "Payment Service 이미지 빌드" \
    "AI Service 이미지 빌드" \
    "Gateway Service 이미지 빌드" \
    "Member DB 시작" \
    "Core DB 시작" \
    "Database Initializer 실행" \
    "Redis 시작" \
    "Kafka 시작" \
    "Elasticsearch 시작" \
    "Config Server 시작" \
    "Member Service 시작" \
    "Core Service 시작" \
    "Payment Service 시작" \
    "AI Service 시작" \
    "Gateway Service 시작"
  # 전체 Gradle 이미지를 동시에 빌드하면 Docker VM 메모리가 고갈될 수 있어 순차 빌드합니다.
  run_step 2 "Spotless 검사 및 자동 수정" run_spotless_if_available
  run_step 4 "Member DB 이미지 준비" prepare_service_image member-db
  run_step 8 "Core DB 이미지 준비" prepare_service_image core-db
  run_step 12 "Database Initializer 이미지 준비" prepare_service_image database-initializer
  run_step 16 "Redis 이미지 준비" prepare_service_image redis
  run_step 20 "Kafka 이미지 준비" prepare_service_image kafka
  run_step 24 "Elasticsearch 이미지 빌드" compose build elasticsearch
  run_step 28 "Config Server 이미지 빌드" compose build config-server
  run_step 32 "Member Service 이미지 빌드" compose build member-service
  run_step 36 "Core Service 이미지 빌드" compose build core-service
  run_step 40 "Payment Service 이미지 빌드" compose build payment-service
  run_step 44 "AI Service 이미지 빌드" compose build ai-service
  run_step 48 "Gateway Service 이미지 빌드" compose build gateway-service
  run_step 52 "Member DB 시작" compose up -d member-db
  run_step 56 "Core DB 시작" compose up -d core-db
  run_step 60 "Database Initializer 실행" compose up -d database-initializer
  run_step 64 "Redis 시작" compose up -d redis
  run_step 68 "Kafka 시작" compose up -d kafka
  run_step 72 "Elasticsearch 시작" compose up -d --no-build elasticsearch
  run_step 76 "Config Server 시작" compose up -d --no-build config-server
  run_step 80 "Member Service 시작" compose up -d --no-build member-service
  run_step 84 "Core Service 시작" compose up -d --no-build core-service
  run_step 88 "Payment Service 시작" compose up -d --no-build payment-service
  run_step 92 "AI Service 시작" compose up -d --no-build ai-service
  run_step 96 "Gateway Service 시작" compose up -d --no-build gateway-service
elif [[ $# -eq 1 && ("$1" == "member-service" || "$1" == "core-service" || "$1" == "payment-service") ]]; then
  selected_service="$1"
  case "$selected_service" in
    member-service)
      selected_display="Member Service"
      selected_db="member-db"
      selected_db_display="Member DB"
      needs_database_initializer=false
      ;;
    core-service)
      selected_display="Core Service"
      selected_db="core-db"
      selected_db_display="Core DB"
      needs_database_initializer=false
      ;;
    payment-service)
      selected_display="Payment Service"
      selected_db="core-db"
      selected_db_display="Core DB"
      needs_database_initializer=true
      ;;
  esac
  final_summary=(
    "$selected_db_display 시작됨"
    "Redis 시작됨"
    "Kafka 시작됨"
    "Config Server 시작됨"
  )
  if [[ "$needs_database_initializer" == true ]]; then
    final_summary+=("Database Initializer 실행됨")
  fi
  final_summary+=("$selected_display 시작됨")

  selected_steps=(
    "Spotless 검사 및 자동 수정"
    "PostgreSQL 이미지 준비"
    "Redis 이미지 준비"
    "Kafka 이미지 준비"
    "Config Server 이미지 빌드"
    "$selected_display 이미지 빌드"
    "$selected_db_display 시작"
  )
  if [[ "$needs_database_initializer" == true ]]; then
    selected_steps+=("Database Initializer 실행")
  fi
  selected_steps+=("Redis 시작" "Kafka 시작" "Config Server 시작" "$selected_display 시작")
  configure_progress_ui "${selected_steps[@]}"

  run_step 2 "Spotless 검사 및 자동 수정" run_spotless_if_available
  run_step 5 "PostgreSQL 이미지 준비" prepare_postgresql_image
  run_step 10 "Redis 이미지 준비" prepare_service_image redis
  run_step 15 "Kafka 이미지 준비" prepare_service_image kafka
  run_step 25 "Config Server 이미지 빌드" compose build config-server
  run_step 35 "$selected_display 이미지 빌드" compose build "$selected_service"
  run_step 45 "$selected_db_display 시작" compose up -d "$selected_db"
  if [[ "$needs_database_initializer" == true ]]; then
    run_step 55 "Database Initializer 실행" compose up -d database-initializer
    run_step 65 "Redis 시작" compose up -d redis
    run_step 75 "Kafka 시작" compose up -d kafka
    run_step 85 "Config Server 시작" compose up -d --no-build config-server
  else
    run_step 60 "Redis 시작" compose up -d redis
    run_step 70 "Kafka 시작" compose up -d kafka
    run_step 85 "Config Server 시작" compose up -d --no-build config-server
  fi
  run_step 95 "$selected_display 시작" compose up -d --no-build "$selected_service"
elif [[ $# -eq 1 && "$1" == "ai-service" ]]; then
  final_summary=(
    "Core DB 시작됨"
    "Database Initializer 실행됨"
    "Redis 시작됨"
    "Kafka 시작됨"
    "Elasticsearch 시작됨"
    "Config Server 시작됨"
    "AI Service 시작됨"
  )
  configure_progress_ui \
    "Spotless 검사 및 자동 수정" \
    "PostgreSQL 이미지 준비" \
    "Redis 이미지 준비" \
    "Kafka 이미지 준비" \
    "Elasticsearch 이미지 빌드" \
    "Config Server 이미지 빌드" \
    "AI Service 이미지 빌드" \
    "Core DB 시작" \
    "Database Initializer 실행" \
    "Redis 시작" \
    "Kafka 시작" \
    "Elasticsearch 시작" \
    "Config Server 시작" \
    "AI Service 시작"
  run_step 2 "Spotless 검사 및 자동 수정" run_spotless_if_available
  run_step 5 "PostgreSQL 이미지 준비" prepare_postgresql_image
  run_step 10 "Redis 이미지 준비" prepare_service_image redis
  run_step 15 "Kafka 이미지 준비" prepare_service_image kafka
  run_step 25 "Elasticsearch 이미지 빌드" compose build elasticsearch
  run_step 35 "Config Server 이미지 빌드" compose build config-server
  run_step 45 "AI Service 이미지 빌드" compose build ai-service
  run_step 50 "Core DB 시작" compose up -d core-db
  run_step 55 "Database Initializer 실행" compose up -d database-initializer
  run_step 60 "Redis 시작" compose up -d redis
  run_step 65 "Kafka 시작" compose up -d kafka
  run_step 75 "Elasticsearch 시작" compose up -d --no-build elasticsearch
  run_step 85 "Config Server 시작" compose up -d --no-build config-server
  run_step 95 "AI Service 시작" compose up -d --no-build ai-service
else
  configure_progress_ui "Spotless 검사 및 자동 수정" "PostgreSQL 이미지 준비" "Redis 이미지 준비" "Kafka 이미지 준비" "이미지 빌드 및 서비스 시작"
  run_step 10 "Spotless 검사 및 자동 수정" run_spotless_if_available
  run_step 20 "PostgreSQL 이미지 준비" prepare_postgresql_image
  run_step 40 "Redis 이미지 준비" prepare_service_image redis
  run_step 60 "Kafka 이미지 준비" prepare_service_image kafka
  run_step 100 "이미지 빌드 및 서비스 시작" compose up -d --build "$@"
fi
if [[ ${#final_summary[@]} -gt 0 ]]; then
  print_final_summary "${final_summary[@]}"
else
  print_progress "완료" 100 "서비스 시작"
fi

# 새 컨테이너가 참조하는 이미지는 보존하고, 이번 빌드로 교체된 이전 이미지만 제거합니다.
after_images="$(compose images -q | sort -u)"
while IFS= read -r image_id; do
  [[ -z "$image_id" ]] && continue
  if ! grep -Fqx "$image_id" <<<"$after_images"; then
    # 다른 컨테이너가 사용 중이면 Docker가 삭제를 거부하므로 강제 삭제하지 않습니다.
    docker image rm "$image_id" >/dev/null 2>&1 || true
  fi
done <<<"$before_images"
