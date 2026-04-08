#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
LOG_FILE="${OUTPUT_DIR}/supervisor.log"
DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"
CHECK_INTERVAL_SECONDS="${OVERNIGHT_CHECK_INTERVAL_SECONDS:-20}"
STALE_SECONDS="${OVERNIGHT_STALE_SECONDS:-4200}"
RUN_SCRIPT="${PROJECT_ROOT}/scripts/overnight-hourly-run.sh"
NOTIFY_SCRIPT="${PROJECT_ROOT}/scripts/feishu_notify.py"
SUPERVISOR_PID_FILE="${STATE_DIR}/autopilot.pid"
RUN_PID_FILE="${STATE_DIR}/current_run.pid"
RUN_META_FILE="${STATE_DIR}/current_run.meta"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"
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

cleanup() {
  rm -f "${SUPERVISOR_PID_FILE}"
}

trap cleanup EXIT

write_supervisor_pid() {
  echo "$$" > "${SUPERVISOR_PID_FILE}"
}

restart_caffeinate() {
  local seconds="$1"
  if [[ -f "${CAFFEINATE_PID_FILE}" ]]; then
    local existing_pid
    existing_pid="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${existing_pid}" ]]; then
      kill "${existing_pid}" >/dev/null 2>&1 || true
    fi
  fi
  pkill -f "caffeinate -dimsu -t" >/dev/null 2>&1 || true
  nohup caffeinate -dimsu -t "${seconds}" >> "${OUTPUT_DIR}/start.log" 2>&1 &
  echo $! > "${CAFFEINATE_PID_FILE}"
  log "已启动保活进程，持续 ${seconds} 秒"
}

ensure_caffeinate_alive() {
  local remaining pid
  remaining="$(remaining_seconds)"
  if [[ ! -f "${CAFFEINATE_PID_FILE}" ]]; then
    restart_caffeinate "${remaining}"
    notify "ShareHub 夜间推进巡检
状态：未找到保活进程，已自动补拉
时间：$(date '+%F %T %z')"
    return
  fi

  pid="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"
  if [[ -z "${pid}" ]] || ! kill -0 "${pid}" >/dev/null 2>&1; then
    restart_caffeinate "${remaining}"
    notify "ShareHub 夜间推进巡检
状态：检测到保活进程已停止，已自动重启
时间：$(date '+%F %T %z')"
  fi
}

ensure_deadline() {
  local current_hour
  current_hour="$(date '+%H' | sed 's/^0*//')"
  current_hour="${current_hour:-0}"
  if [[ "${current_hour}" -ge "${DEADLINE_HOUR}" ]]; then
    log "已到截止时间，停止夜间自动推进"
    stop_current_run_if_any
    if [[ -f "${CAFFEINATE_PID_FILE}" ]]; then
      local pid
      pid="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"
      if [[ -n "${pid}" ]]; then
        kill "${pid}" >/dev/null 2>&1 || true
      fi
      rm -f "${CAFFEINATE_PID_FILE}"
    fi
    notify "ShareHub 夜间推进结束
状态：已到截止时间，自动停止
时间：$(date '+%F %T %z')"
    exit 0
  fi
}

start_run() {
  local reason="$1"
  nohup bash "${RUN_SCRIPT}" >> "${LOG_FILE}" 2>&1 &
  local run_pid=$!
  printf 'RUN_PID=%s\nSTART_EPOCH=%s\nREASON=%s\n' "${run_pid}" "$(date '+%s')" "${reason}" > "${RUN_META_FILE}"
  echo "${run_pid}" > "${RUN_PID_FILE}"
  log "已启动新一轮自动推进，PID=${run_pid}，原因：${reason}"
}

stop_current_run_if_any() {
  if [[ ! -f "${RUN_PID_FILE}" ]]; then
    rm -f "${RUN_META_FILE}"
    return
  fi

  local run_pid
  run_pid="$(cat "${RUN_PID_FILE}" 2>/dev/null || true)"
  if [[ -n "${run_pid}" ]] && kill -0 "${run_pid}" >/dev/null 2>&1; then
    kill "${run_pid}" >/dev/null 2>&1 || true
    sleep 1
    kill -9 "${run_pid}" >/dev/null 2>&1 || true
    log "已停止卡住的单轮执行，PID=${run_pid}"
  fi
  rm -f "${RUN_PID_FILE}" "${RUN_META_FILE}"
}

current_run_pid() {
  cat "${RUN_PID_FILE}" 2>/dev/null || true
}

current_run_start_epoch() {
  awk -F'=' '$1=="START_EPOCH"{print $2; exit}' "${RUN_META_FILE}" 2>/dev/null || true
}

handle_stale_run() {
  local run_pid start_epoch now_epoch
  run_pid="$(current_run_pid)"
  start_epoch="$(current_run_start_epoch)"
  now_epoch="$(date '+%s')"

  if [[ -n "${run_pid}" ]] && kill -0 "${run_pid}" >/dev/null 2>&1; then
    if [[ -n "${start_epoch}" ]] && (( now_epoch - start_epoch > STALE_SECONDS )); then
      notify "ShareHub 夜间推进异常
状态：单轮执行超过 ${STALE_SECONDS} 秒未结束，准备强制重启
运行 PID：${run_pid}
时间：$(date '+%F %T %z')"
      stop_current_run_if_any
      start_run "上一轮超时自愈"
    fi
    return
  fi

  if [[ -n "${run_pid}" ]]; then
    log "检测到上一轮进程已退出，准备自动启动下一轮"
    rm -f "${RUN_PID_FILE}" "${RUN_META_FILE}"
    start_run "上一轮结束后自动续跑"
  fi
}

ensure_run_alive() {
  local run_pid
  run_pid="$(current_run_pid)"
  if [[ -z "${run_pid}" ]]; then
    start_run "启动立即执行"
    return
  fi
  handle_stale_run
}

log "夜间 supervisor 启动"
write_supervisor_pid
ensure_caffeinate_alive
ensure_run_alive

while true; do
  ensure_deadline
  ensure_caffeinate_alive
  ensure_run_alive
  sleep "${CHECK_INTERVAL_SECONDS}"
done
