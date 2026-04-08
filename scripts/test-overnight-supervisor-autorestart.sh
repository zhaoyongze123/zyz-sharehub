#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TEST_ROOT="$(mktemp -d)"
KEEP_TEST_ROOT=0

cleanup() {
  if [[ -n "${SUPERVISOR_PID:-}" ]] && kill -0 "${SUPERVISOR_PID}" >/dev/null 2>&1; then
    kill "${SUPERVISOR_PID}" >/dev/null 2>&1 || true
    wait "${SUPERVISOR_PID}" 2>/dev/null || true
  fi

  if [[ -n "${CURRENT_RUN_PID:-}" ]] && kill -0 "${CURRENT_RUN_PID}" >/dev/null 2>&1; then
    kill "${CURRENT_RUN_PID}" >/dev/null 2>&1 || true
    wait "${CURRENT_RUN_PID}" 2>/dev/null || true
  fi

  if [[ "${KEEP_TEST_ROOT}" -eq 0 ]]; then
    rm -rf "${TEST_ROOT}"
  else
    echo "test root kept at ${TEST_ROOT}" >&2
  fi
}
trap cleanup EXIT

TEST_PROJECT="${TEST_ROOT}/project"
TEST_OUTPUT_DIR="${TEST_PROJECT}/output/overnight"
TEST_STATE_DIR="${TEST_OUTPUT_DIR}/state"
TEST_BIN_DIR="${TEST_ROOT}/bin"

mkdir -p "${TEST_PROJECT}/scripts" "${TEST_STATE_DIR}" "${TEST_BIN_DIR}"

cp "${PROJECT_ROOT}/scripts/overnight-supervisor.sh" "${TEST_PROJECT}/scripts/overnight-supervisor.sh"

cat > "${TEST_PROJECT}/scripts/overnight-hourly-run.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATE_DIR="${PROJECT_ROOT}/output/overnight/state"
COUNTER_FILE="${STATE_DIR}/run-counter.txt"
COUNT=0

if [[ -f "${COUNTER_FILE}" ]]; then
  COUNT="$(cat "${COUNTER_FILE}")"
fi

COUNT=$((COUNT + 1))
printf '%s\n' "${COUNT}" > "${COUNTER_FILE}"
printf 'PID=%s\nCOUNT=%s\n' "$$" "${COUNT}" > "${STATE_DIR}/run-${COUNT}.env"

if [[ "${COUNT}" -eq 1 ]]; then
  exit 0
fi

sleep 30
EOF

cat > "${TEST_PROJECT}/scripts/feishu_notify.py" <<'EOF'
#!/usr/bin/env python3
print("notify skipped")
EOF

cat > "${TEST_BIN_DIR}/caffeinate" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

seconds=60
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "-t" ]]; then
    seconds="$2"
    shift 2
    continue
  fi
  shift
done

sleep "${seconds}"
EOF

chmod +x \
  "${TEST_PROJECT}/scripts/overnight-supervisor.sh" \
  "${TEST_PROJECT}/scripts/overnight-hourly-run.sh" \
  "${TEST_PROJECT}/scripts/feishu_notify.py" \
  "${TEST_BIN_DIR}/caffeinate"

SUPERVISOR_LOG="${TEST_OUTPUT_DIR}/supervisor.log"

env \
  PATH="${TEST_BIN_DIR}:/usr/bin:/bin:/usr/sbin:/sbin" \
  OVERNIGHT_CHECK_INTERVAL_SECONDS=1 \
  OVERNIGHT_STALE_SECONDS=20 \
  OVERNIGHT_DEADLINE_HOUR=9 \
  bash "${TEST_PROJECT}/scripts/overnight-supervisor.sh" \
  > "${TEST_ROOT}/supervisor-stdout.log" 2>&1 &
SUPERVISOR_PID=$!

for _ in $(seq 1 80); do
  if [[ -f "${TEST_STATE_DIR}/run-2.env" ]]; then
    break
  fi
  if ! kill -0 "${SUPERVISOR_PID}" >/dev/null 2>&1; then
    KEEP_TEST_ROOT=1
    echo "supervisor exited unexpectedly"
    cat "${TEST_ROOT}/supervisor-stdout.log" || true
    exit 1
  fi
  sleep 0.2
done

if [[ ! -f "${TEST_STATE_DIR}/run-2.env" ]]; then
  KEEP_TEST_ROOT=1
  echo "second run was not started"
  cat "${SUPERVISOR_LOG}" || true
  exit 1
fi

FIRST_RUN_PID="$(awk -F'=' '$1=="PID"{print $2; exit}' "${TEST_STATE_DIR}/run-1.env")"
SECOND_RUN_PID="$(awk -F'=' '$1=="PID"{print $2; exit}' "${TEST_STATE_DIR}/run-2.env")"
CURRENT_RUN_PID="$(cat "${TEST_STATE_DIR}/current_run.pid")"

[[ -n "${FIRST_RUN_PID}" ]] || { echo "first run pid missing"; exit 1; }
[[ -n "${SECOND_RUN_PID}" ]] || { echo "second run pid missing"; exit 1; }
[[ "${FIRST_RUN_PID}" != "${SECOND_RUN_PID}" ]] || { echo "run pid did not change"; exit 1; }
[[ "${CURRENT_RUN_PID}" == "${SECOND_RUN_PID}" ]] || { echo "current run pid mismatch"; exit 1; }

grep -q '已启动新一轮自动推进，PID=.*原因：启动立即执行' "${SUPERVISOR_LOG}" || {
  echo "initial start log missing"
  exit 1
}
grep -q '检测到上一轮进程已退出，准备自动启动下一轮' "${SUPERVISOR_LOG}" || {
  echo "autorestart detect log missing"
  exit 1
}
grep -q '已启动新一轮自动推进，PID=.*原因：上一轮结束后自动续跑' "${SUPERVISOR_LOG}" || {
  echo "autorestart start log missing"
  exit 1
}

echo "supervisor autorestart test passed"
