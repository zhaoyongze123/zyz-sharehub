#!/usr/bin/env bash
set -euo pipefail

load_env_file() {
  local env_file="$1"
  if [[ -f "${env_file}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
  fi
}

load_env_stack() {
  local base_dir="$1"
  shift
  local name
  for name in "$@"; do
    load_env_file "${base_dir}/${name}"
  done
}
