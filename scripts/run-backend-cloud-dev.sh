#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "${PROJECT_ROOT}/backend/.env.cloud-dev.local" ]]; then
  # shellcheck disable=SC1091
  source "${PROJECT_ROOT}/backend/.env.cloud-dev.local"
fi

: "${POSTGRES_PASSWORD:?请先设置 POSTGRES_PASSWORD，或写入 backend/.env.cloud-dev.local}"
: "${REDIS_PASSWORD:?请先设置 REDIS_PASSWORD，或写入 backend/.env.cloud-dev.local}"

if [[ -d "/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

SPRING_PORT="${SERVER_PORT:-8080}"
SPRING_PROFILES="cloud-dev"

if [[ -n "${GITHUB_CLIENT_ID:-}" && -n "${GITHUB_CLIENT_SECRET:-}" ]]; then
  export GITHUB_OAUTH_ENABLED="${GITHUB_OAUTH_ENABLED:-true}"
  SPRING_PROFILES="cloud-dev,oauth"
fi

cd "${PROJECT_ROOT}/backend"
mvn spring-boot:run \
  -Dspring-boot.run.profiles="${SPRING_PROFILES}" \
  -Dspring-boot.run.arguments="--server.port=${SPRING_PORT}"
