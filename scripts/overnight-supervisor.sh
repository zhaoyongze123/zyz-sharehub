#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
LOG_FILE="${OUTPUT_DIR}/supervisor.log"
DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"
CHECK_INTERVAL_SECONDS="${OVERNIGHT_CHECK_INTERVAL_SECONDS:-300}"
STALE_SECONDS="${OVERNIGHT_STALE_SECONDS:-4200}"
LAST_HOURLY_MARKER_FILE="${STATE_DIR}/last_hourly_marker.txt"
NOTIFY_SCRIPT="${PROJECT_ROOT}/scripts/feishu_notify.py"
export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:${PATH:-}"

mkdir -p "${OUTPUT_DIR}" "${STATE_DIR}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${LOG_FILE}"
}

notify() {
  python3 "${NOTIFY_SCRIPT}" "$1" >> "${LOG_FILE}" 2>&1 || true
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

restart_caffeinate() {
  local seconds="$1"
  pkill -f "caffeinate -dimsu -t" >/dev/null 2>&1 || true
  nohup caffeinate -dimsu -t "${seconds}" >> "${OUTPUT_DIR}/start.log" 2>&1 &
  echo $! > "${STATE_DIR}/caffeinate.pid"
  echo "${seconds}" > "${STATE_DIR}/caffeinate-seconds.txt"
  log "已启动保活进程，持续 ${seconds} 秒"
}

meta_value() {
  local key="$1"
  local file="$2"
  awk -F'=' -v key="$key" '$1 == key { sub($1"=",""); print; exit }' "$file"
}

latest_end_epoch() {
  local meta_file="$1"
  local end_at
  end_at="$(meta_value "END_AT" "${meta_file}")"
  if [[ -z "${end_at}" ]]; then
    echo ""
    return 0
  fi

  python3 - "${end_at}" <<'PY'
from datetime import datetime
import sys

value = sys.argv[1]
try:
    print(int(datetime.strptime(value, "%Y-%m-%d %H:%M:%S %z").timestamp()))
except ValueError:
    print("")
PY
}

trigger_run() {
  local reason="$1"
  log "触发一轮自动推进，原因：${reason}"
  if "${PROJECT_ROOT}/scripts/launchd-overnight-entry.sh" >> "${LOG_FILE}" 2>&1; then
    log "自动推进触发成功：${reason}"
  else
    log "自动推进触发失败：${reason}"
    notify "ShareHub 夜间推进异常
状态：触发执行失败
原因：${reason}
时间：$(date '+%F %T %z')"
  fi
}

current_hour_marker() {
  date '+%Y%m%d%H'
}

current_minute() {
  date '+%M'
}

mark_hourly_run() {
  current_hour_marker > "${LAST_HOURLY_MARKER_FILE}"
}

need_hourly_run() {
  local marker minute last_marker
  marker="$(current_hour_marker)"
  minute="$(current_minute)"
  last_marker="$(cat "${LAST_HOURLY_MARKER_FILE}" 2>/dev/null || true)"

  if [[ ! -f "${LAST_HOURLY_MARKER_FILE}" ]]; then
    return 0
  fi

  if [[ "${marker}" != "${last_marker}" && "${minute}" -le 5 ]]; then
    return 0
  fi

  return 1
}

ensure_caffeinate_alive() {
  local remaining
  remaining="$(remaining_seconds)"
  if [[ ! -f "${STATE_DIR}/caffeinate.pid" ]]; then
    restart_caffeinate "${remaining}"
    notify "ShareHub 夜间推进巡检
状态：未找到保活进程，已自动补拉
时间：$(date '+%F %T %z')"
    return
  fi

  local pid
  pid="$(cat "${STATE_DIR}/caffeinate.pid")"
  if ! kill -0 "${pid}" >/dev/null 2>&1; then
    restart_caffeinate "${remaining}"
    notify "ShareHub 夜间推进巡检
状态：检测到保活进程已停止，已自动重启
时间：$(date '+%F %T %z')"
  fi
}

ensure_stale_recovery() {
  local meta_file="${OUTPUT_DIR}/latest-meta.env"
  if [[ ! -f "${meta_file}" ]]; then
    trigger_run "缺少 latest-meta.env"
    return
  fi

  local end_epoch now_epoch run_id
  end_epoch="$(latest_end_epoch "${meta_file}")"
  run_id="$(meta_value "RUN_ID" "${meta_file}")"
  now_epoch="$(date '+%s')"
  if [[ -n "${end_epoch}" ]] && (( now_epoch - end_epoch > STALE_SECONDS )); then
    trigger_run "最近一轮超过 ${STALE_SECONDS} 秒未更新"
    notify "ShareHub 夜间推进巡检
状态：最近一轮执行已超时未更新，已触发补跑
最近轮次：${run_id:-unknown}
时间：$(date '+%F %T %z')"
  fi
}

ensure_deadline() {
  if [[ "$(date '+%H' | sed 's/^0*//')" -ge "${DEADLINE_HOUR}" ]]; then
    log "已到截止时间，停止夜间自动推进"
    pkill -f "caffeinate -dimsu -t" >/dev/null 2>&1 || true
    notify "ShareHub 夜间推进结束
状态：已到截止时间，自动停止
时间：$(date '+%F %T %z')"
    exit 0
  fi
}

log "夜间 supervisor 启动"
ensure_caffeinate_alive
trigger_run "启动立即执行"
mark_hourly_run

while true; do
  ensure_deadline
  ensure_caffeinate_alive
  ensure_stale_recovery

  if need_hourly_run; then
    trigger_run "整点小时批次"
    mark_hourly_run
  fi

  sleep "${CHECK_INTERVAL_SECONDS}"
done
