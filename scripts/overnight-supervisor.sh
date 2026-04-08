#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
RUN_LOCK_DIR="${STATE_DIR}/hourly-run.lock"
RUN_LOCK_META_FILE="${RUN_LOCK_DIR}/owner.env"
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

DEADLINE_EPOCH="$(python3 - "$DEADLINE_HOUR" <<'PY'
from datetime import datetime, timedelta
import sys

deadline_hour = int(sys.argv[1])
now = datetime.now()
target = now.replace(hour=deadline_hour, minute=0, second=0, microsecond=0)
if now >= target:
    target += timedelta(days=1)
print(int(target.timestamp()))
PY
)"

mkdir -p "${OUTPUT_DIR}" "${STATE_DIR}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${LOG_FILE}"
}

notify() {
  python3 "${NOTIFY_SCRIPT}" "$1" >> "${LOG_FILE}" 2>&1 || true
}

remaining_seconds() {
  local now
  now="$(date '+%s')"
  if [[ "${now}" -ge "${DEADLINE_EPOCH}" ]]; then
    echo 0
  else
    echo $(( DEADLINE_EPOCH - now ))
  fi
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
    python3 "${NOTIFY_SCRIPT}" \
      --event "保活巡检" \
      --status "已恢复" \
      --reason "未找到保活进程" \
      --action "已自动补拉 caffeinate" \
      --evidence "${LOG_FILE}" \
      >> "${LOG_FILE}" 2>&1 || true
    return
  fi

  pid="$(cat "${CAFFEINATE_PID_FILE}" 2>/dev/null || true)"
  if [[ -z "${pid}" ]] || ! kill -0 "${pid}" >/dev/null 2>&1; then
    restart_caffeinate "${remaining}"
    python3 "${NOTIFY_SCRIPT}" \
      --event "保活巡检" \
      --status "已恢复" \
      --reason "检测到保活进程已停止" \
      --action "已自动重启 caffeinate" \
      --evidence "${LOG_FILE}" \
      >> "${LOG_FILE}" 2>&1 || true
  fi
}

ensure_deadline() {
  if [[ "$(date '+%s')" -ge "${DEADLINE_EPOCH}" ]]; then
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
    python3 "${NOTIFY_SCRIPT}" \
      --event "夜间推进结束" \
      --status "成功" \
      --reason "已到截止时间，自动停止" \
      --action "结束 supervisor 与保活进程" \
      --evidence "${LOG_FILE}" \
      >> "${LOG_FILE}" 2>&1 || true
    exit 0
  fi
}

start_run() {
  local reason="$1"
  nohup env OVERNIGHT_RUN_SOURCE=supervisor bash "${RUN_SCRIPT}" >> "${LOG_FILE}" 2>&1 &
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

active_locked_run_pid() {
  if [[ ! -f "${RUN_LOCK_META_FILE}" ]]; then
    echo ""
    return
  fi

  local lock_pid
  lock_pid="$(awk -F'=' '$1=="PID"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"
  if [[ -n "${lock_pid}" ]] && kill -0 "${lock_pid}" >/dev/null 2>&1; then
    echo "${lock_pid}"
    return
  fi

  rm -rf "${RUN_LOCK_DIR}"
  echo ""
}

handle_stale_run() {
  local run_pid start_epoch now_epoch
  run_pid="$(current_run_pid)"
  start_epoch="$(current_run_start_epoch)"
  now_epoch="$(date '+%s')"

  if [[ -n "${run_pid}" ]] && kill -0 "${run_pid}" >/dev/null 2>&1; then
    if [[ -n "${start_epoch}" ]] && (( now_epoch - start_epoch > STALE_SECONDS )); then
      python3 "${NOTIFY_SCRIPT}" \
        --event "单轮超时自愈" \
        --status "需关注" \
        --stage "supervisor" \
        --reason "单轮执行超过 ${STALE_SECONDS} 秒未结束" \
        --action "准备强制重启当前轮次，PID=${run_pid}" \
        --evidence "${LOG_FILE}" \
        >> "${LOG_FILE}" 2>&1 || true
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
  local lock_pid
  lock_pid="$(active_locked_run_pid)"
  if [[ -n "${lock_pid}" ]]; then
    if [[ -z "${run_pid}" ]] || [[ "${run_pid}" != "${lock_pid}" ]]; then
      log "检测到已有轮次锁，PID=${lock_pid}，本次不重复拉起新轮次"
    fi
    return
  fi
  if [[ -z "${run_pid}" ]]; then
    start_run "启动立即执行"
    return
  fi
  handle_stale_run
}

log "夜间 supervisor 启动，截止时间 $(date -r "${DEADLINE_EPOCH}" '+%F %T %z')"
write_supervisor_pid
ensure_caffeinate_alive
ensure_run_alive

while true; do
  ensure_deadline
  ensure_caffeinate_alive
  ensure_run_alive
  sleep "${CHECK_INTERVAL_SECONDS}"
done
