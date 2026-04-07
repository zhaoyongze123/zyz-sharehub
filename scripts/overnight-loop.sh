#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
mkdir -p "${STATE_DIR}" "${OUTPUT_DIR}"

DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"
HEARTBEAT_FILE="${STATE_DIR}/heartbeat.txt"
LOOP_LOG="${OUTPUT_DIR}/loop.log"

now_epoch() {
  date '+%s'
}

deadline_epoch() {
  python3 - "$DEADLINE_HOUR" <<'PY'
from datetime import datetime, timedelta
import sys

deadline_hour = int(sys.argv[1])
now = datetime.now()
target = now.replace(hour=deadline_hour, minute=0, second=0, microsecond=0)
if now >= target:
    target = target + timedelta(days=1)
print(int(target.timestamp()))
PY
}

sleep_until_next_hour() {
  python3 - <<'PY'
from datetime import datetime, timedelta
now = datetime.now()
target = (now.replace(minute=0, second=0, microsecond=0) + timedelta(hours=1))
print(max(1, int((target - now).total_seconds())))
PY
}

TARGET_EPOCH="$(deadline_epoch)"
echo "[$(date '+%F %T')] 夜间自动推进启动，截止到 $(date -r "${TARGET_EPOCH}" '+%F %T')" | tee -a "${LOOP_LOG}"

while true; do
  CURRENT_EPOCH="$(now_epoch)"
  if [[ "${CURRENT_EPOCH}" -ge "${TARGET_EPOCH}" ]]; then
    echo "[$(date '+%F %T')] 已到截止时间，停止夜间推进" | tee -a "${LOOP_LOG}"
    break
  fi

  echo "[$(date '+%F %T')] 触发一轮自动推进" | tee -a "${LOOP_LOG}"
  if "${PROJECT_ROOT}/scripts/overnight-hourly-run.sh" >> "${LOOP_LOG}" 2>&1; then
    echo "[$(date '+%F %T')] 本轮执行成功" | tee -a "${LOOP_LOG}"
  else
    echo "[$(date '+%F %T')] 本轮执行失败，但保留循环继续下一轮" | tee -a "${LOOP_LOG}"
  fi

  date '+%F %T %z' > "${HEARTBEAT_FILE}"
  SLEEP_SECONDS="$(sleep_until_next_hour)"
  echo "[$(date '+%F %T')] 休眠 ${SLEEP_SECONDS} 秒，等待下个整点" | tee -a "${LOOP_LOG}"
  sleep "${SLEEP_SECONDS}"
done
