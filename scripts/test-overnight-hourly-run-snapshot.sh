#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TEST_ROOT="$(mktemp -d)"
KEEP_TEST_ROOT=0
cleanup() {
  if [[ "${KEEP_TEST_ROOT}" -eq 0 ]]; then
    rm -rf "${TEST_ROOT}"
  else
    echo "test root kept at ${TEST_ROOT}" >&2
  fi
}
trap cleanup EXIT

TEST_PROJECT="${TEST_ROOT}/project"
TEST_REMOTE="${TEST_ROOT}/remote.git"
mkdir -p "${TEST_PROJECT}/scripts" "${TEST_PROJECT}/output/overnight/state" "${TEST_ROOT}/bin"

cp "${PROJECT_ROOT}/scripts/overnight-hourly-run.sh" "${TEST_PROJECT}/scripts/overnight-hourly-run.sh"
cp "${PROJECT_ROOT}/scripts/load-env.sh" "${TEST_PROJECT}/scripts/load-env.sh"
cp "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TEST_PROJECT}/scripts/run_with_timeout.py"

cat > "${TEST_PROJECT}/scripts/overnight-manager-prompt.md" <<'EOF'
# test prompt
EOF

cat > "${TEST_PROJECT}/scripts/overnight-browser-smoke.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
RUN_DIR="${1:?run dir required}"
SMOKE_DIR="${RUN_DIR}/browser-smoke"
mkdir -p "${SMOKE_DIR}"
cat > "${SMOKE_DIR}/meta.env" <<'META'
ADMIN_SMOKE_EXIT_CODE=0
ADMIN_GATE_EXIT_CODE=0
META
echo "smoke-ok" >> "${RUN_DIR}/smoke-ran.log"
EOF

cat > "${TEST_ROOT}/bin/fake-codex" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

output_file=""
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "-o" ]]; then
    output_file="$2"
    shift 2
    continue
  fi
  shift
done

if [[ -z "${output_file}" ]]; then
  echo "missing -o output file" >&2
  exit 2
fi

printf 'fake codex ok\n' > "${output_file}"
EOF

chmod +x \
  "${TEST_PROJECT}/scripts/overnight-hourly-run.sh" \
  "${TEST_PROJECT}/scripts/load-env.sh" \
  "${TEST_PROJECT}/scripts/overnight-browser-smoke.sh" \
  "${TEST_PROJECT}/scripts/run_with_timeout.py" \
  "${TEST_ROOT}/bin/fake-codex"

git -C "${TEST_PROJECT}" init -q
git -C "${TEST_PROJECT}" config user.name "ShareHub Test"
git -C "${TEST_PROJECT}" config user.email "sharehub-test@example.com"
git -C "${TEST_PROJECT}" add scripts
git -C "${TEST_PROJECT}" commit -q -m "init"
git -C "${TEST_PROJECT}" branch -M test-branch
git init --bare -q "${TEST_REMOTE}"
git -C "${TEST_PROJECT}" remote add origin "${TEST_REMOTE}"
git -C "${TEST_PROJECT}" push -q -u origin test-branch

env -u OVERNIGHT_PROJECT_ROOT -u OVERNIGHT_RUN_SNAPSHOT_ACTIVE \
  HOME="${TEST_ROOT}/home" \
  OVERNIGHT_CODEX_BIN="${TEST_ROOT}/bin/fake-codex" \
  "${TEST_PROJECT}/scripts/overnight-hourly-run.sh" \
  >"${TEST_ROOT}/hourly-run.log" 2>&1 &
RUN_PID=$!

for _ in $(seq 1 50); do
  SNAPSHOT_PATH="${TEST_PROJECT}/output/overnight/state/hourly-run.exec.sh"
  if [[ -f "${SNAPSHOT_PATH}" ]]; then
    break
  fi
  if ! kill -0 "${RUN_PID}" >/dev/null 2>&1; then
    break
  fi
  sleep 0.1
done

if [[ ! -f "${SNAPSHOT_PATH}" ]]; then
  KEEP_TEST_ROOT=1
  echo "snapshot script was not created"
  cat "${TEST_ROOT}/hourly-run.log" || true
  wait "${RUN_PID}" || true
  exit 1
fi

printf '#!/usr/bin/env bash\nexit 99\n' > "${TEST_PROJECT}/scripts/overnight-hourly-run.sh"

wait "${RUN_PID}"
RUN_EXIT_CODE=$?
if [[ ${RUN_EXIT_CODE} -ne 0 ]]; then
  KEEP_TEST_ROOT=1
  cat "${TEST_ROOT}/hourly-run.log" || true
  exit "${RUN_EXIT_CODE}"
fi

LATEST_MESSAGE="${TEST_PROJECT}/output/overnight/latest-message.md"
LATEST_META="${TEST_PROJECT}/output/overnight/latest-meta.env"
PUSH_LOG="$(find "${TEST_PROJECT}/output/overnight" -name git-push.txt -print -quit)"

[[ -f "${LATEST_MESSAGE}" ]] || { echo "latest message missing"; exit 1; }
[[ -f "${LATEST_META}" ]] || { echo "latest meta missing"; exit 1; }
[[ -n "${PUSH_LOG}" ]] || { echo "push log missing"; exit 1; }

grep -q 'fake codex ok' "${LATEST_MESSAGE}" || { echo "codex output missing"; exit 1; }
grep -q 'ADMIN_AUTH_EXIT_CODE=0' "${LATEST_META}" || { echo "admin auth exit code missing"; exit 1; }
grep -q 'ADMIN_SMOKE_EXIT_CODE=0' "${LATEST_META}" || { echo "admin smoke exit code missing"; exit 1; }
grep -q 'ADMIN_GATE_EXIT_CODE=0' "${LATEST_META}" || { echo "admin gate exit code missing"; exit 1; }
grep -q 'ADMIN_SMOKE_SCRIPT_EXIT_CODE=0' "${LATEST_META}" || { echo "admin smoke script exit code missing"; exit 1; }
grep -q 'PARALLEL_DEGRADED_REASON=none' "${LATEST_META}" || { echo "parallel degraded reason missing"; exit 1; }
! grep -q '^SMOKE_EXIT_CODE=' "${LATEST_META}" || { echo "legacy smoke exit code should be absent"; exit 1; }
grep -q 'FRONTEND_FOLLOWUP_EXIT_CODE=DISABLED' "${LATEST_META}" || { echo "frontend followup disable flag missing"; exit 1; }
grep -q 'PUSH_STATUS=SUCCESS' "${LATEST_META}" || { echo "push status missing"; exit 1; }
grep -q 'Everything up-to-date' "${PUSH_LOG}" || { echo "push output missing"; exit 1; }
grep -q 'CODEX_HOME=' "${LATEST_META}" || { echo "codex home missing"; exit 1; }
grep -q '已禁用公开站点前端跟进子代理' "${TEST_ROOT}/hourly-run.log" || { echo "admin-only disable log missing"; exit 1; }

echo "snapshot hourly run test passed"
