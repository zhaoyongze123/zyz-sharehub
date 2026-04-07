#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
LOG_FILE="${OUTPUT_DIR}/launchd.log"
DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"
LAUNCHD_LABEL="com.sharehub.overnight.autopilot"
PLIST_PATH="${HOME}/Library/LaunchAgents/${LAUNCHD_LABEL}.plist"

mkdir -p "${OUTPUT_DIR}"

CURRENT_HOUR="$(date '+%H' | sed 's/^0*//')"
CURRENT_HOUR="${CURRENT_HOUR:-0}"

if [[ "${CURRENT_HOUR}" -ge "${DEADLINE_HOUR}" ]]; then
  echo "[$(date '+%F %T')] 已到截止时间，卸载 LaunchAgent ${LAUNCHD_LABEL}" >> "${LOG_FILE}"
  launchctl bootout "gui/$(id -u)" "${PLIST_PATH}" >/dev/null 2>&1 || true
  exit 0
fi

echo "[$(date '+%F %T')] launchd 触发一轮自动推进" >> "${LOG_FILE}"
if "${PROJECT_ROOT}/scripts/overnight-hourly-run.sh" >> "${LOG_FILE}" 2>&1; then
  echo "[$(date '+%F %T')] 本轮 launchd 执行成功" >> "${LOG_FILE}"
else
  echo "[$(date '+%F %T')] 本轮 launchd 执行失败，但保留下一轮调度" >> "${LOG_FILE}"
fi

exit 0
