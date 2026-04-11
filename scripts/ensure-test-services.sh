#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "${PROJECT_ROOT}/scripts/load-env.sh"
load_env_stack "${PROJECT_ROOT}/backend" ".env.test" ".env.test.local"

COMPOSE_FILE="${PROJECT_ROOT}/deploy/docker-compose.test-services.yml"
POSTGRES_CONTAINER="sharehub-test-postgres"
REDIS_CONTAINER="sharehub-test-redis"

: "${POSTGRES_TEST_PASSWORD:?请先设置 POSTGRES_TEST_PASSWORD，或写入 backend/.env.test.local}"
: "${REDIS_TEST_PASSWORD:?请先设置 REDIS_TEST_PASSWORD，或写入 backend/.env.test.local}"

docker compose -f "${COMPOSE_FILE}" up -d >/dev/null

wait_for_health() {
  local container_name="$1"
  local retries="${2:-60}"
  local delay="${3:-2}"
  local attempt=1

  while (( attempt <= retries )); do
    local status
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_name}" 2>/dev/null || true)"
    if [[ "${status}" == "healthy" || "${status}" == "running" ]]; then
      return 0
    fi
    sleep "${delay}"
    attempt=$(( attempt + 1 ))
  done

  echo "测试服务未在预期时间内就绪: ${container_name}" >&2
  docker ps --filter "name=${container_name}" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' >&2 || true
  return 1
}

wait_for_health "${POSTGRES_CONTAINER}"
wait_for_health "${REDIS_CONTAINER}"

verify_postgres_credentials() {
  docker exec \
    -e PGPASSWORD="${POSTGRES_TEST_PASSWORD}" \
    "${POSTGRES_CONTAINER}" \
    psql -v ON_ERROR_STOP=1 -U "${POSTGRES_TEST_USER}" -d "${POSTGRES_TEST_DB}" -c 'SELECT 1;' \
    >/dev/null 2>&1
}

verify_redis_credentials() {
  docker exec "${REDIS_CONTAINER}" redis-cli -a "${REDIS_TEST_PASSWORD}" PING >/dev/null 2>&1
}

if ! verify_postgres_credentials || ! verify_redis_credentials; then
  echo "检测到测试服务凭据与当前 .env 不一致，正在重建测试卷..." >&2
  docker compose -f "${COMPOSE_FILE}" down -v >/dev/null
  docker compose -f "${COMPOSE_FILE}" up -d >/dev/null
  wait_for_health "${POSTGRES_CONTAINER}"
  wait_for_health "${REDIS_CONTAINER}"

  if ! verify_postgres_credentials || ! verify_redis_credentials; then
    echo "测试服务重建后仍无法通过凭据校验" >&2
    exit 1
  fi
fi
