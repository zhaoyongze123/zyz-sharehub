#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LAUNCHD_LABEL="com.sharehub.overnight.autopilot"
PLIST_PATH="${HOME}/Library/LaunchAgents/${LAUNCHD_LABEL}.plist"
CAFFEINATE_PID_FILE="${PROJECT_ROOT}/output/overnight/state/caffeinate.pid"

launchctl bootout "gui/$(id -u)" "${PLIST_PATH}" >/dev/null 2>&1 || true
echo "已卸载 LaunchAgent：${LAUNCHD_LABEL}"

if [[ -f "${CAFFEINATE_PID_FILE}" ]]; then
  CAFFEINATE_PID="$(cat "${CAFFEINATE_PID_FILE}")"
  if kill -0 "${CAFFEINATE_PID}" >/dev/null 2>&1; then
    kill "${CAFFEINATE_PID}" || true
    echo "已停止保活进程，PID=${CAFFEINATE_PID}"
  fi
  rm -f "${CAFFEINATE_PID_FILE}"
fi

rm -f "${PROJECT_ROOT}/output/overnight/state/caffeinate-seconds.txt"
