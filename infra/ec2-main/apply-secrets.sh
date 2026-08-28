#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <app|data|platform|monitoring>" >&2
  exit 1
fi

namespace="$1"
case "$namespace" in
  app|data|platform|monitoring) ;;
  *)
    echo "Unsupported namespace: $namespace" >&2
    exit 1
    ;;
esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="${script_dir}/namespaces/runtime"
env_dir="${runtime_dir}/env"
example_dir="${runtime_dir}/examples"
env_file="${env_dir}/${namespace}.env"
secret_name="${namespace}-runtime-secrets"

if [[ ! -f "$env_file" ]]; then
  echo "Missing ${env_file}; copy ${example_dir}/${namespace}.env.example and fill every value." >&2
  exit 1
fi

if awk -F= 'NF < 2 || $1 == "" || substr($0, index($0, "=") + 1) == "" { invalid=1 } END { exit !invalid }' "$env_file"; then
  echo "Every key in ${env_file} must have a value." >&2
  exit 1
fi

registry_file="${env_dir}/registry.env"
additional_env_args=()
if [[ "$namespace" == "app" ]]; then
  openai_env_file="${env_dir}/app-openai.env"
  if [[ ! -f "$openai_env_file" ]]; then
    echo "Missing ${openai_env_file}; copy ${example_dir}/app-openai.env.example and fill every value." >&2
    exit 1
  fi
  if awk -F= 'NF < 2 || $1 == "" || substr($0, index($0, "=") + 1) == "" { invalid=1 } END { exit !invalid }' "$openai_env_file"; then
    echo "Every key in ${openai_env_file} must have a value." >&2
    exit 1
  fi
  additional_env_args+=(--from-env-file="$openai_env_file")
fi
if [[ "$namespace" == "app" || "$namespace" == "platform" ]]; then
  if [[ ! -f "$registry_file" ]]; then
    echo "Missing ${registry_file}; copy ${example_dir}/registry.env.example and fill every value." >&2
    exit 1
  fi
  if awk -F= 'NF < 2 || $1 == "" || substr($0, index($0, "=") + 1) == "" { invalid=1 } END { exit !invalid }' "$registry_file"; then
    echo "Every key in ${registry_file} must have a value." >&2
    exit 1
  fi
fi

temporary_acl=""
temporary_manifest="$(mktemp)"
temporary_updated_manifest="$(mktemp)"
cleanup() {
  rm -f "$temporary_manifest" "$temporary_updated_manifest"
  if [[ -n "$temporary_acl" ]]; then
    rm -f "$temporary_acl"
  fi
}
trap cleanup EXIT

if [[ "$namespace" == "app" ]]; then
  private_key="${env_dir}/access-private-key.pem"
  public_key="${env_dir}/access-public-key.pem"
  if [[ ! -f "$private_key" || ! -f "$public_key" ]]; then
    echo "app requires access-private-key.pem and access-public-key.pem in ${env_dir}." >&2
    exit 1
  fi
elif [[ "$namespace" == "data" ]]; then
  redis_password="$(awk -F= '$1 == "REDIS_PASSWORD" { print substr($0, index($0, "=") + 1) }' "$env_file")"
  temporary_acl="$(mktemp)"
  chmod 600 "$temporary_acl"
  printf 'user default on >%s ~* &* +@all\n' "$redis_password" > "$temporary_acl"
fi

kubectl create secret generic "$secret_name" \
  --namespace="$namespace" \
  --from-env-file="$env_file" \
  "${additional_env_args[@]}" \
  --dry-run=client -o json > "$temporary_manifest"

if [[ "$namespace" == "app" ]]; then
  jq \
    --rawfile private_key "$private_key" \
    --rawfile public_key "$public_key" \
    '.data["access-private-key.pem"] = ($private_key | @base64)
     | .data["access-public-key.pem"] = ($public_key | @base64)' \
    "$temporary_manifest" > "$temporary_updated_manifest"
  mv "$temporary_updated_manifest" "$temporary_manifest"
elif [[ "$namespace" == "data" ]]; then
  jq \
    --rawfile redis_acl "$temporary_acl" \
    '.data["users.acl"] = ($redis_acl | @base64)' \
    "$temporary_manifest" > "$temporary_updated_manifest"
  mv "$temporary_updated_manifest" "$temporary_manifest"
fi

kubectl apply -f "$temporary_manifest"

if [[ "$namespace" == "app" || "$namespace" == "platform" ]]; then
  ghcr_username="$(awk -F= '$1 == "GHCR_USERNAME" { print substr($0, index($0, "=") + 1) }' "$registry_file")"
  ghcr_token="$(awk -F= '$1 == "GHCR_TOKEN" { print substr($0, index($0, "=") + 1) }' "$registry_file")"
  kubectl create secret docker-registry ghcr-pull-secret \
    --namespace="$namespace" \
    --docker-server=ghcr.io \
    --docker-username="$ghcr_username" \
    --docker-password="$ghcr_token" \
    --dry-run=client -o yaml | kubectl apply -f -
fi

echo "Applied secrets for ${namespace}. Restart workloads that consume them."
