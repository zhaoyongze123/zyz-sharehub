#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="${PROJECT_ROOT}/deploy"
ENV_FILE="${DEPLOY_DIR}/.env.staging"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.staging.yml"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-sharehub-staging}"
HEALTH_URL="${STAGING_HEALTHCHECK_URL:-http://127.0.0.1:19081/actuator/health}"
MAX_RETRIES="${STAGING_HEALTHCHECK_RETRIES:-30}"
SLEEP_SECONDS="${STAGING_HEALTHCHECK_INTERVAL:-5}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[staging-deploy] 缺少环境文件: ${ENV_FILE}" >&2
  exit 1
fi

if [[ ! -d "${PROJECT_ROOT}/frontend/dist" ]]; then
  echo "[staging-deploy] 缺少前端构建产物: ${PROJECT_ROOT}/frontend/dist" >&2
  exit 1
fi

echo "[staging-deploy] 启动 compose 部署"
cd "${DEPLOY_DIR}"
docker compose -p "${PROJECT_NAME}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build

echo "[staging-deploy] 等待健康检查: ${HEALTH_URL}"
attempt=1
until curl -fsS "${HEALTH_URL}" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; do
  if (( attempt >= MAX_RETRIES )); then
    echo "[staging-deploy] 健康检查失败，输出 compose 状态与日志" >&2
    docker compose -p "${PROJECT_NAME}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps >&2
    docker compose -p "${PROJECT_NAME}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 >&2
    exit 1
  fi

  echo "[staging-deploy] 第 ${attempt} 次检查未通过，${SLEEP_SECONDS}s 后重试"
  sleep "${SLEEP_SECONDS}"
  attempt=$((attempt + 1))
done

echo "[staging-deploy] 健康检查通过"
docker compose -p "${PROJECT_NAME}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps
