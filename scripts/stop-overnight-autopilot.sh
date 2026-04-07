#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATE_DIR="${PROJECT_ROOT}/output/overnight/state"
PID_FILE="${STATE_DIR}/autopilot.pid"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"

if [[ -f "${PID_FILE}" ]]; then
  PID="$(cat "${PID_FILE}")"
  if kill -0 "${PID}" >/dev/null 2>&1; then
    kill "${PID}" || true
    echo "已停止 supervisor，PID=${PID}"
  fi
  rm -f "${PID_FILE}"
fi

if [[ -f "${CAFFEINATE_PID_FILE}" ]]; then
  CAFFEINATE_PID="$(cat "${CAFFEINATE_PID_FILE}")"
  if kill -0 "${CAFFEINATE_PID}" >/dev/null 2>&1; then
    kill "${CAFFEINATE_PID}" || true
    echo "已停止保活进程，PID=${CAFFEINATE_PID}"
  fi
  rm -f "${CAFFEINATE_PID_FILE}"
fi

rm -f "${STATE_DIR}/caffeinate-seconds.txt"
pkill -f "overnight-supervisor.sh" >/dev/null 2>&1 || true
pkill -f "launchd-overnight-entry.sh" >/dev/null 2>&1 || true
pkill -f "run_with_timeout.py .*New project" >/dev/null 2>&1 || true
pkill -f "codex exec --dangerously-bypass-approvals-and-sandbox -C /Users/mac/Documents/New project" >/dev/null 2>&1 || true
