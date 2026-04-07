#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
PID_FILE="${STATE_DIR}/autopilot.pid"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"
START_LOG="${OUTPUT_DIR}/start.log"

mkdir -p "${STATE_DIR}" "${OUTPUT_DIR}"

chmod +x \
  "${PROJECT_ROOT}/scripts/launchd-overnight-entry.sh" \
  "${PROJECT_ROOT}/scripts/overnight-hourly-run.sh" \
  "${PROJECT_ROOT}/scripts/overnight-monitor.sh" \
  "${PROJECT_ROOT}/scripts/overnight-supervisor.sh" \
  "${PROJECT_ROOT}/scripts/feishu_notify.py" \
  "${PROJECT_ROOT}/scripts/run_with_timeout.py"

if [[ -f "${PID_FILE}" ]]; then
  EXISTING_PID="$(cat "${PID_FILE}")"
  if kill -0 "${EXISTING_PID}" >/dev/null 2>&1; then
    echo "夜间自动推进已在运行，PID=${EXISTING_PID}"
    exit 0
  fi
fi

nohup bash -lc "cd '${PROJECT_ROOT}' && exec ./scripts/overnight-supervisor.sh" >> "${START_LOG}" 2>&1 &
SUPERVISOR_PID=$!
echo "${SUPERVISOR_PID}" > "${PID_FILE}"

sleep 1
CAFFEINATE_PID="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"

echo "已启动夜间自动推进，supervisor PID=${SUPERVISOR_PID}"
if [[ -n "${CAFFEINATE_PID}" ]]; then
  echo "保活进程 PID=${CAFFEINATE_PID}"
fi
echo "日志目录：${OUTPUT_DIR}"
