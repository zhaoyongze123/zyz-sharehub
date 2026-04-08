#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
SUPERVISOR_PID_FILE="${STATE_DIR}/autopilot.pid"
RUN_PID_FILE="${STATE_DIR}/current_run.pid"
RUN_META_FILE="${STATE_DIR}/current_run.meta"
LATEST_META_FILE="${OUTPUT_DIR}/latest-meta.env"

echo "== ShareHub 夜间自动推进诊断 =="
echo "project_root=${PROJECT_ROOT}"
echo

if [[ -f "${SUPERVISOR_PID_FILE}" ]]; then
  SUPERVISOR_PID="$(cat "${SUPERVISOR_PID_FILE}" 2>/dev/null || true)"
  echo "supervisor_pid=${SUPERVISOR_PID:-missing}"
  if [[ -n "${SUPERVISOR_PID}" ]] && kill -0 "${SUPERVISOR_PID}" >/dev/null 2>&1; then
    echo "supervisor_status=RUNNING"
  else
    echo "supervisor_status=STOPPED"
  fi
else
  echo "supervisor_pid=missing"
  echo "supervisor_status=STOPPED"
fi

echo

if [[ -f "${RUN_PID_FILE}" ]]; then
  RUN_PID="$(cat "${RUN_PID_FILE}" 2>/dev/null || true)"
  echo "run_pid=${RUN_PID:-missing}"
  if [[ -n "${RUN_PID}" ]] && kill -0 "${RUN_PID}" >/dev/null 2>&1; then
    echo "run_status=RUNNING"
  else
    echo "run_status=STOPPED"
  fi
else
  echo "run_pid=missing"
  echo "run_status=IDLE"
fi

echo

if [[ -f "${RUN_META_FILE}" ]]; then
  echo "-- current_run.meta --"
  cat "${RUN_META_FILE}"
  echo
fi

if [[ -f "${LATEST_META_FILE}" ]]; then
  echo "-- latest-meta.env --"
  cat "${LATEST_META_FILE}"
  echo
fi
