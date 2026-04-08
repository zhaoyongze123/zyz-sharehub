#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
PID_FILE="${STATE_DIR}/autopilot.pid"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"
START_LOG="${OUTPUT_DIR}/start.log"

mkdir -p "${STATE_DIR}" "${OUTPUT_DIR}"

export OVERNIGHT_DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-23}"

chmod +x \
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

rm -f "${PID_FILE}"

python3 - "${PROJECT_ROOT}" "${START_LOG}" <<'PY'
from pathlib import Path
import subprocess
import sys

project_root = Path(sys.argv[1])
start_log = Path(sys.argv[2])

with start_log.open("ab") as log_file:
    subprocess.Popen(
        ["bash", "-lc", "exec ./scripts/overnight-supervisor.sh"],
        cwd=project_root,
        stdin=subprocess.DEVNULL,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
PY

SUPERVISOR_PID=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  if [[ -f "${PID_FILE}" ]]; then
    SUPERVISOR_PID="$(cat "${PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${SUPERVISOR_PID}" ]] && kill -0 "${SUPERVISOR_PID}" >/dev/null 2>&1; then
      break
    fi
  fi
  sleep 1
done

if [[ -z "${SUPERVISOR_PID}" ]] || ! kill -0 "${SUPERVISOR_PID}" >/dev/null 2>&1; then
  echo "夜间自动推进启动失败，请检查 ${START_LOG} 和 output/overnight/supervisor.log"
  exit 1
fi

CAFFEINATE_PID="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"

echo "已启动夜间自动推进，supervisor PID=${SUPERVISOR_PID}"
if [[ -n "${CAFFEINATE_PID}" ]]; then
  echo "保活进程 PID=${CAFFEINATE_PID}"
fi
echo "日志目录：${OUTPUT_DIR}"
