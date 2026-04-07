#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
LAUNCHD_LABEL="com.sharehub.overnight.autopilot"
PLIST_PATH="${HOME}/Library/LaunchAgents/${LAUNCHD_LABEL}.plist"
CAFFEINATE_PID_FILE="${STATE_DIR}/caffeinate.pid"
START_LOG="${OUTPUT_DIR}/start.log"
DEADLINE_HOUR="${OVERNIGHT_DEADLINE_HOUR:-9}"

mkdir -p "${STATE_DIR}" "${OUTPUT_DIR}"

mkdir -p "${HOME}/Library/LaunchAgents"

cat > "${PLIST_PATH}" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>${LAUNCHD_LABEL}</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/bash</string>
    <string>-lc</string>
    <string>cd '${PROJECT_ROOT}' &amp;&amp; exec ./scripts/launchd-overnight-entry.sh</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>StartCalendarInterval</key>
  <dict>
    <key>Minute</key>
    <integer>0</integer>
  </dict>
  <key>StandardOutPath</key>
  <string>${START_LOG}</string>
  <key>StandardErrorPath</key>
  <string>${START_LOG}</string>
</dict>
</plist>
PLIST

launchctl bootout "gui/$(id -u)" "${PLIST_PATH}" >/dev/null 2>&1 || true
launchctl bootstrap "gui/$(id -u)" "${PLIST_PATH}"

nohup bash -lc "cd '${PROJECT_ROOT}' && exec ./scripts/launchd-overnight-entry.sh" >> "${START_LOG}" 2>&1 &

python3 - "${DEADLINE_HOUR}" <<'PY' > "${STATE_DIR}/caffeinate-seconds.txt"
from datetime import datetime, timedelta
import sys

deadline_hour = int(sys.argv[1])
now = datetime.now()
target = now.replace(hour=deadline_hour, minute=0, second=0, microsecond=0)
if now >= target:
    target += timedelta(days=1)
print(max(1, int((target - now).total_seconds())))
PY

CAFFEINATE_SECONDS="$(cat "${STATE_DIR}/caffeinate-seconds.txt")"
nohup caffeinate -dimsu -t "${CAFFEINATE_SECONDS}" >> "${START_LOG}" 2>&1 &
CAFFEINATE_PID=$!
echo "${CAFFEINATE_PID}" > "${CAFFEINATE_PID_FILE}"

echo "已启动夜间自动推进，LaunchAgent=${LAUNCHD_LABEL}"
echo "保活进程 PID=${CAFFEINATE_PID}，持续 ${CAFFEINATE_SECONDS} 秒"
echo "日志目录：${OUTPUT_DIR}"
