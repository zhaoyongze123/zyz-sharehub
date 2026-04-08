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

BACKEND_PORT="${OVERNIGHT_BACKEND_PORT:-18080}"
FRONTEND_PORT="${OVERNIGHT_FRONTEND_PORT:-14173}"
BACKEND_BASE_URL="http://127.0.0.1:${BACKEND_PORT}"
FRONTEND_BASE_URL="http://127.0.0.1:${FRONTEND_PORT}"

mkdir -p "${SMOKE_DIR}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${SMOKE_LOG}"
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

collect_modules() {
  local files=""
  if [[ -n "${START_HEAD}" && -n "${END_HEAD}" ]]; then
    files="$(git -C "${PROJECT_ROOT}" diff --name-only "${START_HEAD}".."${END_HEAD}" || true)"
  fi

  if [[ -z "${files}" ]]; then
    files="$(git -C "${PROJECT_ROOT}" diff --name-only || true)"
  fi

  local untracked_files
  untracked_files="$(git -C "${PROJECT_ROOT}" ls-files --others --exclude-standard || true)"
  if [[ -n "${untracked_files}" ]]; then
    if [[ -n "${files}" ]]; then
      files="${files}"$'\n'"${untracked_files}"
    else
      files="${untracked_files}"
    fi
  fi

  python3 - <<'PY' "${files}"
import sys

files = [line.strip() for line in sys.argv[1].splitlines() if line.strip()]
modules = set()
frontend_changed = False
frontend_global_changed = False

for path in files:
    lowered = path.lower()
    if "frontend/" in lowered or lowered.startswith("frontend"):
        frontend_changed = True
        if any(token in lowered for token in (
            "frontend/package.json",
            "frontend/package-lock.json",
            "frontend/vite.config",
            "frontend/playwright.config",
            "frontend/tests/",
            "frontend/src/api/"
        )):
            frontend_global_changed = True
        if "resource" in lowered:
            modules.add("resources")
        if "roadmap" in lowered:
            modules.add("roadmaps")
        if "note" in lowered or "community" in lowered:
            modules.add("notes")
        if "resume" in lowered:
            modules.add("resumes")
        if "admin" in lowered:
            modules.add("admin")
        if "user/" in lowered or "/me" in lowered or "profile" in lowered or "auth" in lowered:
            modules.add("profile")
    if path.startswith("backend/"):
        modules.add("backend")

if frontend_changed and (frontend_global_changed or not modules.intersection({"resources", "roadmaps", "notes", "resumes", "admin", "profile"})):
    modules.update({"resources", "roadmaps", "notes", "resumes", "admin", "profile"})

if not modules:
    modules.update({"public", "backend"})
else:
    modules.add("public")

print(",".join(sorted(modules)))
PY
}

cleanup() {
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

MODULES="${PLAYWRIGHT_MODULES:-$(collect_modules)}"
log "本轮浏览器 smoke 模块：${MODULES}"

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

if ! wait_for_url "${BACKEND_BASE_URL}/actuator/health" "${BACKEND_PID}" "后端" 120 2; then
  log "后端健康检查失败，请查看 ${BACKEND_LOG}"
  exit 1
fi

export VITE_API_PROXY_TARGET="${BACKEND_BASE_URL}"
nohup bash -lc "cd '${FRONTEND_DIR}' && npm run dev -- --host 127.0.0.1 --port ${FRONTEND_PORT} --strictPort" > "${FRONTEND_LOG}" 2>&1 &
FRONTEND_PID=$!
log "前端已启动，PID=${FRONTEND_PID}"

if ! wait_for_url "${FRONTEND_BASE_URL}" "${FRONTEND_PID}" "前端" 90 2; then
  log "前端健康检查失败，请查看 ${FRONTEND_LOG}"
  exit 1
fi

export PLAYWRIGHT_BASE_URL="${FRONTEND_BASE_URL}"
export PLAYWRIGHT_API_BASE_URL="${BACKEND_BASE_URL}"
export PLAYWRIGHT_MODULES="${MODULES}"
export PLAYWRIGHT_HTML_REPORT="${SMOKE_DIR}/playwright-report"
export VITE_API_PROXY_TARGET="${BACKEND_BASE_URL}"

log "开始执行 Playwright smoke"
(cd "${FRONTEND_DIR}" && npm run test:e2e) | tee -a "${SMOKE_LOG}"

cat > "${META_FILE}" <<EOF
BACKEND_PORT=${BACKEND_PORT}
FRONTEND_PORT=${FRONTEND_PORT}
MODULES=${MODULES}
BACKEND_BASE_URL=${BACKEND_BASE_URL}
FRONTEND_BASE_URL=${FRONTEND_BASE_URL}
BACKEND_MODE=${BACKEND_MODE}
EOF

log "Playwright smoke 执行完成"
