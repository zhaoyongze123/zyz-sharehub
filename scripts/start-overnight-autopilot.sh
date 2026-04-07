#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
PID_FILE="${STATE_DIR}/autopilot.pid"
START_LOG="${OUTPUT_DIR}/start.log"

mkdir -p "${STATE_DIR}" "${OUTPUT_DIR}"

if [[ -f "${PID_FILE}" ]]; then
  EXISTING_PID="$(cat "${PID_FILE}")"
  if kill -0 "${EXISTING_PID}" >/dev/null 2>&1; then
    echo "自动推进已在运行，PID=${EXISTING_PID}"
    exit 0
  fi
fi

nohup bash -lc "cd '${PROJECT_ROOT}' && exec caffeinate -dimsu ./scripts/overnight-loop.sh" >> "${START_LOG}" 2>&1 &
AUTOPILOT_PID=$!
echo "${AUTOPILOT_PID}" > "${PID_FILE}"

echo "已启动夜间自动推进，PID=${AUTOPILOT_PID}"
echo "日志目录：${OUTPUT_DIR}"
