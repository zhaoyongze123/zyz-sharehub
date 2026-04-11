#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "${PROJECT_ROOT}/scripts/load-env.sh"
load_env_stack "${PROJECT_ROOT}/backend" ".env.cloud-dev" ".env.cloud-dev.local"

: "${POSTGRES_PASSWORD:?请先设置 POSTGRES_PASSWORD，或写入 backend/.env.cloud-dev.local}"
: "${REDIS_PASSWORD:?请先设置 REDIS_PASSWORD，或写入 backend/.env.cloud-dev.local}"

if [[ -d "/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

SPRING_PORT="${SERVER_PORT:-8080}"

cd "${PROJECT_ROOT}/backend"
mvn spring-boot:run \
  -Dspring-boot.run.profiles=cloud-dev \
  -Dspring-boot.run.arguments="--server.port=${SPRING_PORT}"
