#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "${PROJECT_ROOT}/scripts/load-env.sh"
load_env_stack "${PROJECT_ROOT}/backend" ".env.test" ".env.test.local"

POSTGRES_CONTAINER="sharehub-test-postgres"
REDIS_CONTAINER="sharehub-test-redis"

: "${POSTGRES_TEST_DB:?请先设置 POSTGRES_TEST_DB，或写入 backend/.env.test.local}"
: "${POSTGRES_TEST_USER:?请先设置 POSTGRES_TEST_USER，或写入 backend/.env.test.local}"
: "${POSTGRES_TEST_PASSWORD:?请先设置 POSTGRES_TEST_PASSWORD，或写入 backend/.env.test.local}"
: "${REDIS_TEST_PASSWORD:?请先设置 REDIS_TEST_PASSWORD，或写入 backend/.env.test.local}"

docker exec \
  -e PGPASSWORD="${POSTGRES_TEST_PASSWORD}" \
  "${POSTGRES_CONTAINER}" \
  psql -v ON_ERROR_STOP=1 -U "${POSTGRES_TEST_USER}" -d "${POSTGRES_TEST_DB}" \
  -c 'DROP SCHEMA IF EXISTS public CASCADE;' \
  -c 'CREATE SCHEMA public;' \
  -c "GRANT ALL ON SCHEMA public TO ${POSTGRES_TEST_USER};" \
  -c 'GRANT ALL ON SCHEMA public TO public;' \
  >/dev/null

docker exec "${REDIS_CONTAINER}" redis-cli -a "${REDIS_TEST_PASSWORD}" FLUSHALL >/dev/null
