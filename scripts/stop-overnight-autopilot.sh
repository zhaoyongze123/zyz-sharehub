#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PID_FILE="${PROJECT_ROOT}/output/overnight/state/autopilot.pid"
CAFFEINATE_PID_FILE="${PROJECT_ROOT}/output/overnight/state/caffeinate.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "没有找到运行中的自动推进 PID 文件"
  exit 0
fi

PID="$(cat "${PID_FILE}")"
if kill -0 "${PID}" >/dev/null 2>&1; then
  kill "${PID}"
  echo "已停止自动推进，PID=${PID}"
else
  echo "PID=${PID} 不存在，清理残留 PID 文件"
fi

if [[ -f "${CAFFEINATE_PID_FILE}" ]]; then
  CAFFEINATE_PID="$(cat "${CAFFEINATE_PID_FILE}")"
  if kill -0 "${CAFFEINATE_PID}" >/dev/null 2>&1; then
    kill "${CAFFEINATE_PID}" || true
    echo "已停止保活进程，PID=${CAFFEINATE_PID}"
  fi
  rm -f "${CAFFEINATE_PID_FILE}"
fi

rm -f "${PID_FILE}"
