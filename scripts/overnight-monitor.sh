#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
LOG_FILE="${OUTPUT_DIR}/monitor.log"
RUN_LABEL="com.sharehub.overnight.autopilot.hourly"
MONITOR_LABEL="com.sharehub.overnight.autopilot.monitor"
RUN_PLIST="${HOME}/Library/LaunchAgents/${RUN_LABEL}.plist"
MONITOR_PLIST="${HOME}/Library/LaunchAgents/${MONITOR_LABEL}.plist"
DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"
STALE_SECONDS="${OVERNIGHT_STALE_SECONDS:-4200}"
export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:${PATH:-}"

mkdir -p "${OUTPUT_DIR}" "${STATE_DIR}"

log() {
  echo "[$(date '+%F %T')] $*" >> "${LOG_FILE}"
}

send_issue() {
  python3 "${PROJECT_ROOT}/scripts/feishu_notify.py" "$1" >> "${LOG_FILE}" 2>&1 || true
}

meta_value() {
  local key="$1"
  local file="$2"
  awk -F'=' -v key="$key" '$1 == key { sub($1"=",""); print; exit }' "$file"
}

ensure_bootstrapped() {
  local label="$1"
  local plist="$2"
  if ! launchctl print "gui/$(id -u)/${label}" >/dev/null 2>&1; then
    log "检测到 ${label} 未加载，尝试 bootstrap"
    launchctl bootstrap "gui/$(id -u)" "${plist}" >> "${LOG_FILE}" 2>&1 || true
  fi
}

restart_caffeinate() {
  local seconds="$1"
  pkill -f "caffeinate -dimsu -t" >/dev/null 2>&1 || true
  nohup caffeinate -dimsu -t "${seconds}" >> "${OUTPUT_DIR}/start.log" 2>&1 &
  echo $! > "${STATE_DIR}/caffeinate.pid"
  echo "${seconds}" > "${STATE_DIR}/caffeinate-seconds.txt"
  log "已重启 caffeinate，持续 ${seconds} 秒"
}

remaining_seconds() {
  python3 - "${DEADLINE_HOUR}" <<'PY'
from datetime import datetime, timedelta
import sys

deadline_hour = int(sys.argv[1])
now = datetime.now()
target = now.replace(hour=deadline_hour, minute=0, second=0, microsecond=0)
if now >= target:
    target += timedelta(days=1)
print(max(0, int((target - now).total_seconds())))
PY
}

if [[ "$(date '+%H' | sed 's/^0*//')" -ge "${DEADLINE_HOUR}" ]]; then
  log "到达截止时间，停止 hourly 与 monitor LaunchAgent"
  launchctl bootout "gui/$(id -u)" "${RUN_PLIST}" >/dev/null 2>&1 || true
  launchctl bootout "gui/$(id -u)" "${MONITOR_PLIST}" >/dev/null 2>&1 || true
  pkill -f "caffeinate -dimsu -t" >/dev/null 2>&1 || true
  exit 0
fi

ensure_bootstrapped "${RUN_LABEL}" "${RUN_PLIST}"
ensure_bootstrapped "${MONITOR_LABEL}" "${MONITOR_PLIST}"

REMAINING="$(remaining_seconds)"
if [[ -f "${STATE_DIR}/caffeinate.pid" ]]; then
  CPID="$(cat "${STATE_DIR}/caffeinate.pid")"
  if ! kill -0 "${CPID}" >/dev/null 2>&1; then
    log "检测到 caffeinate 不在运行，准备重启"
    restart_caffeinate "${REMAINING}"
    send_issue "ShareHub 夜间推进巡检
状态：检测到保活进程已停止，已自动重启
时间：$(date '+%F %T %z')"
  fi
else
  log "未找到 caffeinate.pid，准备补拉"
  restart_caffeinate "${REMAINING}"
fi

LATEST_META="${OUTPUT_DIR}/latest-meta.env"
if [[ -f "${LATEST_META}" ]]; then
  END_AT_VALUE="$(meta_value "END_AT" "${LATEST_META}")"
  RUN_ID_VALUE="$(meta_value "RUN_ID" "${LATEST_META}")"
  if [[ -n "${END_AT_VALUE}" ]]; then
    LAST_END_EPOCH="$(python3 - "${END_AT_VALUE}" <<'PY'
from datetime import datetime
import sys

value = sys.argv[1]
try:
    print(int(datetime.strptime(value, "%Y-%m-%d %H:%M:%S %z").timestamp()))
except ValueError:
    print("")
PY
)"
    NOW_EPOCH="$(date '+%s')"
    if [[ -n "${LAST_END_EPOCH}" ]] && (( NOW_EPOCH - LAST_END_EPOCH > STALE_SECONDS )); then
      log "检测到最近执行已超过 ${STALE_SECONDS} 秒，触发补跑"
      "${PROJECT_ROOT}/scripts/launchd-overnight-entry.sh" >> "${LOG_FILE}" 2>&1 || true
      send_issue "ShareHub 夜间推进巡检
状态：最近一轮执行已超时未更新，已触发补跑
最近轮次：${RUN_ID_VALUE:-unknown}
时间：$(date '+%F %T %z')"
    fi
  fi
else
  log "缺少 latest-meta.env，触发首次补跑"
  "${PROJECT_ROOT}/scripts/launchd-overnight-entry.sh" >> "${LOG_FILE}" 2>&1 || true
fi
