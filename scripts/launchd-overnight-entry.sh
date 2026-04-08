#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
LOG_FILE="${OUTPUT_DIR}/launchd.log"

mkdir -p "${OUTPUT_DIR}"

echo "[$(date '+%F %T')] 手动单轮入口触发" >> "${LOG_FILE}"
if "${PROJECT_ROOT}/scripts/overnight-hourly-run.sh" >> "${LOG_FILE}" 2>&1; then
  echo "[$(date '+%F %T')] 单轮入口执行成功" >> "${LOG_FILE}"
else
  echo "[$(date '+%F %T')] 单轮入口执行失败" >> "${LOG_FILE}"
fi

exit 0
