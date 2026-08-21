#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <config-server-repository-root>" >&2
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source_dir="${script_dir}/config"
config_server_root="$(cd "$1" && pwd)"
files=(
  application.yml
  member-service.yml
  core-service.yml
  payment-service.yml
  ai-service.yml
  gateway-service.yml
)

if [[ ! -d "${config_server_root}/.git" ]]; then
  echo "Config Server root is not a Git repository: ${config_server_root}" >&2
  exit 1
fi

for file in "${files[@]}"; do
  cp "${source_dir}/${file}" "${config_server_root}/${file}"
done

echo "Copied ${#files[@]} Config Server files to ${config_server_root}"
git -C "${config_server_root}" status --short -- "${files[@]}"
