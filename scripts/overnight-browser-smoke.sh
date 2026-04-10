#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="${1:?请传入 run 目录}"
START_HEAD="${2:-}"
END_HEAD="${3:-}"

FRONTEND_DIR="${PROJECT_ROOT}/frontend"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
SMOKE_DIR="${RUN_DIR}/browser-smoke"
BACKEND_LOG="${SMOKE_DIR}/backend.log"
FRONTEND_LOG="${SMOKE_DIR}/frontend.log"
SMOKE_LOG="${SMOKE_DIR}/smoke.log"
META_FILE="${SMOKE_DIR}/meta.env"
BACKEND_MODE_FILE="${SMOKE_DIR}/backend-mode.txt"
ADMIN_AUTOPILOT_MODE="${OVERNIGHT_ADMIN_AUTOPILOT:-1}"
ADMIN_REQUIRE_POSTGRES="${OVERNIGHT_ADMIN_REQUIRE_POSTGRES:-1}"
STANDARD_SMOKE_SPECS=(
  "tests/e2e/module-smoke.spec.ts"
  "tests/e2e/sharehub-real-api.spec.ts"
)
ADMIN_ALLOWED_ROUTES=(
  "/admin"
  "/admin/reports"
  "/admin/reviews"
  "/admin/users"
  "/admin/audit-logs"
)

BACKEND_PORT="${OVERNIGHT_BACKEND_PORT:-18080}"
FRONTEND_PORT="${OVERNIGHT_FRONTEND_PORT:-14173}"
BACKEND_BASE_URL="http://127.0.0.1:${BACKEND_PORT}"
FRONTEND_BASE_URL="http://127.0.0.1:${FRONTEND_PORT}"
ADMIN_SMOKE_EXIT_CODE=1
ADMIN_GATE_EXIT_CODE=1
STANDARD_SMOKE_EXIT_CODE=1
FAILURE_REASON=""
LAST_HEALTH_STAGE="bootstrap"
FINAL_EXIT_CODE=1

mkdir -p "${SMOKE_DIR}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${SMOKE_LOG}"
}

gate_check() {
  local message="$1"
  FAILURE_REASON="${message}"
  log "后台门禁失败：${message}"
  ADMIN_GATE_EXIT_CODE=1
}

record_failure() {
  local message="$1"
  FAILURE_REASON="${message}"
  log "${message}"
}

write_meta() {
  cat > "${META_FILE}" <<EOF
BACKEND_PORT=${BACKEND_PORT}
FRONTEND_PORT=${FRONTEND_PORT}
MODULES=${MODULES:-unknown}
BACKEND_BASE_URL=${BACKEND_BASE_URL}
FRONTEND_BASE_URL=${FRONTEND_BASE_URL}
BACKEND_MODE=${BACKEND_MODE:-unknown}
ADMIN_AUTOPILOT_MODE=${ADMIN_AUTOPILOT_MODE}
ADMIN_REQUIRE_POSTGRES=${ADMIN_REQUIRE_POSTGRES}
ADMIN_SMOKE_EXIT_CODE=${ADMIN_SMOKE_EXIT_CODE}
ADMIN_GATE_EXIT_CODE=${ADMIN_GATE_EXIT_CODE}
STANDARD_SMOKE_EXIT_CODE=${STANDARD_SMOKE_EXIT_CODE}
FINAL_EXIT_CODE=${FINAL_EXIT_CODE}
LAST_HEALTH_STAGE=${LAST_HEALTH_STAGE}
FAILURE_REASON=${FAILURE_REASON}
EOF
}

has_enabled_property() {
  local path="$1"
  local key="$2"
  [[ -f "${path}" ]] || return 1
  rg -q "^[[:space:]]*${key}:[[:space:]]*true[[:space:]]*$" "${path}"
}

can_use_cloud_dev() {
  if [[ -f "${PROJECT_ROOT}/backend/.env.cloud-dev.local" ]]; then
    # shellcheck disable=SC1091
    source "${PROJECT_ROOT}/backend/.env.cloud-dev.local"
  fi

  if [[ -z "${POSTGRES_PASSWORD:-}" ]]; then
    return 1
  fi

  if [[ -z "${REDIS_PASSWORD:-}" ]]; then
    return 1
  fi

  return 0
}

wait_for_url() {
  local url="$1"
  local pid="$2"
  local label="$3"
  local retries="${4:-90}"
  local delay="${5:-2}"
  local attempt=1
  while (( attempt <= retries )); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "${pid}" ]] && ! kill -0 "${pid}" >/dev/null 2>&1; then
      log "${label}进程已退出，停止等待 ${url}"
      return 1
    fi
    sleep "${delay}"
    attempt=$(( attempt + 1 ))
  done
  return 1
}

wait_for_stable_url() {
  local url="$1"
  local pid="$2"
  local label="$3"
  local checks="${4:-3}"
  local delay="${5:-1}"
  local success_count=0

  while (( success_count < checks )); do
    if [[ -n "${pid}" ]] && ! kill -0 "${pid}" >/dev/null 2>&1; then
      log "${label}进程已退出，稳定性校验失败 ${url}"
      return 1
    fi

    if curl -fsS "${url}" >/dev/null 2>&1; then
      success_count=$(( success_count + 1 ))
      sleep "${delay}"
      continue
    fi

    success_count=0
    sleep "${delay}"
  done

  return 0
}

kill_port_process() {
  local port="$1"
  local pids
  pids="$(lsof -ti tcp:"${port}" 2>/dev/null || true)"
  if [[ -n "${pids}" ]]; then
    kill ${pids} >/dev/null 2>&1 || true
    sleep 1
    pids="$(lsof -ti tcp:"${port}" 2>/dev/null || true)"
    if [[ -n "${pids}" ]]; then
      kill -9 ${pids} >/dev/null 2>&1 || true
    fi
  fi
}

cleanup() {
  FINAL_EXIT_CODE=$?
  if [[ ${FINAL_EXIT_CODE} -eq 0 ]]; then
    ADMIN_SMOKE_EXIT_CODE=${STANDARD_SMOKE_EXIT_CODE}
    if [[ ${ADMIN_GATE_EXIT_CODE} -eq 0 ]]; then
      FAILURE_REASON="${FAILURE_REASON:-none}"
    fi
  fi
  write_meta
  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "${FRONTEND_PID}" >/dev/null 2>&1; then
    kill "${FRONTEND_PID}" >/dev/null 2>&1 || true
    wait "${FRONTEND_PID}" 2>/dev/null || true
  fi
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "${BACKEND_PID}" >/dev/null 2>&1; then
    kill "${BACKEND_PID}" >/dev/null 2>&1 || true
    wait "${BACKEND_PID}" 2>/dev/null || true
  fi
}

trap cleanup EXIT

MODULES="${PLAYWRIGHT_MODULES:-admin,backend}"
log "本轮浏览器 smoke 模块：${MODULES}"
log "后台专项目标页面：${ADMIN_ALLOWED_ROUTES[*]}"

kill_port_process "${BACKEND_PORT}"
kill_port_process "${FRONTEND_PORT}"

export SERVER_PORT="${BACKEND_PORT}"
if can_use_cloud_dev; then
  BACKEND_MODE="cloud-dev"
  nohup bash "${PROJECT_ROOT}/scripts/run-backend-cloud-dev.sh" > "${BACKEND_LOG}" 2>&1 &
else
  BACKEND_MODE="test-profile"
  log "未检测到 cloud-dev 密码配置，回落到本机自包含 smoke profile"
  nohup bash "${PROJECT_ROOT}/scripts/run-backend-smoke.sh" > "${BACKEND_LOG}" 2>&1 &
fi
BACKEND_PID=$!
printf '%s\n' "${BACKEND_MODE}" > "${BACKEND_MODE_FILE}"
log "后端已启动，PID=${BACKEND_PID}，模式=${BACKEND_MODE}"

LAST_HEALTH_STAGE="health"
if ! wait_for_url "${BACKEND_BASE_URL}/actuator/health" "${BACKEND_PID}" "后端" 120 2; then
  record_failure "后端健康检查失败，请查看 ${BACKEND_LOG}"
  exit 1
fi

if ! wait_for_stable_url "${BACKEND_BASE_URL}/actuator/health" "${BACKEND_PID}" "后端" 2 1; then
  record_failure "后端稳定性校验失败，请查看 ${BACKEND_LOG}"
  exit 1
fi

LAST_HEALTH_STAGE="readiness"
if ! wait_for_url "${BACKEND_BASE_URL}/actuator/health/readiness" "${BACKEND_PID}" "后端 readiness" 60 2; then
  record_failure "后端 readiness 检查失败，请查看 ${BACKEND_LOG}"
  exit 1
fi

LAST_HEALTH_STAGE="liveness"
if ! wait_for_url "${BACKEND_BASE_URL}/actuator/health/liveness" "${BACKEND_PID}" "后端 liveness" 60 2; then
  record_failure "后端 liveness 检查失败，请查看 ${BACKEND_LOG}"
  exit 1
fi

export VITE_API_PROXY_TARGET="${BACKEND_BASE_URL}"
nohup bash -lc "cd '${FRONTEND_DIR}' && npm run dev -- --host 127.0.0.1 --port ${FRONTEND_PORT} --strictPort" > "${FRONTEND_LOG}" 2>&1 &
FRONTEND_PID=$!
log "前端已启动，PID=${FRONTEND_PID}"

LAST_HEALTH_STAGE="frontend-health"
if ! wait_for_url "${FRONTEND_BASE_URL}" "${FRONTEND_PID}" "前端" 90 2; then
  record_failure "前端健康检查失败，请查看 ${FRONTEND_LOG}"
  exit 1
fi

if ! wait_for_stable_url "${FRONTEND_BASE_URL}" "${FRONTEND_PID}" "前端" 3 1; then
  record_failure "前端稳定性校验失败，请查看 ${FRONTEND_LOG}"
  exit 1
fi

export PLAYWRIGHT_BASE_URL="${FRONTEND_BASE_URL}"
export PLAYWRIGHT_API_BASE_URL="${BACKEND_BASE_URL}"
export PLAYWRIGHT_MODULES="${MODULES}"
export PLAYWRIGHT_ADMIN_USER_KEY="${PLAYWRIGHT_ADMIN_USER_KEY:-playwright-admin}"
export PLAYWRIGHT_HTML_REPORT="${SMOKE_DIR}/playwright-report"
export VITE_API_PROXY_TARGET="${BACKEND_BASE_URL}"

log "开始执行 Playwright smoke"
set +e
(cd "${FRONTEND_DIR}" && npx playwright test "${STANDARD_SMOKE_SPECS[@]}") | tee -a "${SMOKE_LOG}"
STANDARD_SMOKE_EXIT_CODE=${PIPESTATUS[0]}
set -e

ADMIN_SMOKE_EXIT_CODE=${STANDARD_SMOKE_EXIT_CODE}
ADMIN_GATE_EXIT_CODE=0

if [[ "${ADMIN_AUTOPILOT_MODE}" == "1" ]]; then
  if ! has_enabled_property "${PROJECT_ROOT}/backend/src/main/resources/application-cloud-dev.yml" "dev-token-enabled"; then
    gate_check "application-cloud-dev.yml 未显式开启 dev token"
  fi

  if has_enabled_property "${PROJECT_ROOT}/backend/src/main/resources/application.yml" "dev-token-enabled"; then
    gate_check "application.yml 仍默认开启 dev token"
  fi

  if ! rg -q "jdbc:postgresql" "${PROJECT_ROOT}/backend/src/main/resources/application.yml"; then
    gate_check "application.yml 未使用 PostgreSQL JDBC"
  fi

  if [[ "${ADMIN_REQUIRE_POSTGRES}" == "1" ]]; then
    if rg -q "jdbc:mysql|mysql:" "${PROJECT_ROOT}/deploy/docker-compose.prod.yml"; then
      gate_check "生产部署仍然包含 MySQL 配置，未满足 PostgreSQL-only"
    fi
  fi

  if ! curl -fsS -H "X-User-Key:${PLAYWRIGHT_ADMIN_USER_KEY}" "${BACKEND_BASE_URL}/api/admin/reports?page=1&pageSize=1" >/dev/null 2>&1; then
    gate_check "管理员身份访问后台举报列表失败"
  fi

  if curl -fsS -H "X-User-Key:${PLAYWRIGHT_USER_KEY:-playwright-user}" "${BACKEND_BASE_URL}/api/admin/reports?page=1&pageSize=1" >/dev/null 2>&1; then
    gate_check "普通用户仍可访问后台举报列表"
  fi

  AUTH_ME_BODY="$(curl -fsS -H "X-User-Key:${PLAYWRIGHT_ADMIN_USER_KEY}" "${BACKEND_BASE_URL}/api/auth/me" || true)"
  if [[ "${AUTH_ME_BODY}" != *'"isAdmin":true'* ]]; then
    gate_check "auth/me 未返回 isAdmin=true"
  fi

  if curl -fsS -H "X-Admin-Token:dev-admin-token" "${BACKEND_BASE_URL}/api/admin/reports?page=1&pageSize=1" >/dev/null 2>&1; then
    gate_check "生产环境错误接受了 X-Admin-Token"
  fi

  if ! curl -fsS "${BACKEND_BASE_URL}/actuator/health/readiness" >/dev/null 2>&1; then
    gate_check "readiness probe 不可用"
  fi

  if ! curl -fsS "${BACKEND_BASE_URL}/actuator/health/liveness" >/dev/null 2>&1; then
    gate_check "liveness probe 不可用"
  fi

  if rg -q "PLAYWRIGHT_ADMIN_TOKEN" "${FRONTEND_DIR}/tests/e2e/module-smoke.spec.ts" "${FRONTEND_DIR}/tests/e2e/sharehub-real-api.spec.ts"; then
    gate_check "后台 Playwright 仍依赖 PLAYWRIGHT_ADMIN_TOKEN"
  fi

  if rg -q "/admin/taxonomy|full-site-walkthrough\\.spec\\.ts" \
    "${FRONTEND_DIR}/tests/e2e/module-smoke.spec.ts" \
    "${FRONTEND_DIR}/tests/e2e/sharehub-real-api.spec.ts" \
    "${PROJECT_ROOT}/scripts/overnight-browser-smoke.sh"; then
    gate_check "后台 smoke 仍包含非专项页面或全站走查"
  fi
fi

if [[ ${STANDARD_SMOKE_EXIT_CODE} -ne 0 ]]; then
  record_failure "Playwright smoke 失败，退出码=${STANDARD_SMOKE_EXIT_CODE}"
  exit "${STANDARD_SMOKE_EXIT_CODE}"
fi

if [[ ${ADMIN_GATE_EXIT_CODE} -ne 0 ]]; then
  FAILURE_REASON="${FAILURE_REASON:-后台门禁失败}"
  exit "${ADMIN_GATE_EXIT_CODE}"
fi
FAILURE_REASON="none"
LAST_HEALTH_STAGE="completed"

log "Playwright smoke 执行完成"
