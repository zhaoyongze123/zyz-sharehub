#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATE_DIR="${PROJECT_ROOT}/output/overnight/state"
PID_FILE="${STATE_DIR}/autopilot.pid"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"
RUN_PID_FILE="${STATE_DIR}/current_run.pid"
RUN_LOCK_DIR="${STATE_DIR}/hourly-run.lock"
RUN_LOCK_META_FILE="${RUN_LOCK_DIR}/owner.env"

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
if [[ -f "${RUN_PID_FILE}" ]]; then
  RUN_PID="$(cat "${RUN_PID_FILE}")"
  if kill -0 "${RUN_PID}" >/dev/null 2>&1; then
    kill "${RUN_PID}" || true
    echo "已停止当前轮次执行，PID=${RUN_PID}"
  fi
  rm -f "${RUN_PID_FILE}" "${STATE_DIR}/current_run.meta"
fi

if [[ -f "${RUN_LOCK_META_FILE}" ]]; then
  LOCK_PID="$(awk -F'=' '$1=="PID"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"
  if [[ -n "${LOCK_PID}" ]] && kill -0 "${LOCK_PID}" >/dev/null 2>&1; then
    kill "${LOCK_PID}" >/dev/null 2>&1 || true
    echo "已停止持锁轮次，PID=${LOCK_PID}"
  fi
fi
rm -rf "${RUN_LOCK_DIR}"

pkill -f "overnight-supervisor.sh" >/dev/null 2>&1 || true
pkill -f "run_with_timeout.py .*New project" >/dev/null 2>&1 || true
pkill -f "codex exec --dangerously-bypass-approvals-and-sandbox -C /Users/mac/Documents/New project" >/dev/null 2>&1 || true
