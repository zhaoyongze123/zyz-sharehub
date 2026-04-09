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
RUN_DIR="${TEST_PROJECT}/output/overnight/20260408-frontend-followup"
RUN_DIR_REBASE="${TEST_PROJECT}/output/overnight/20260408-frontend-followup-rebase"

mkdir -p \
  "${TEST_PROJECT}/scripts" \
  "${TEST_PROJECT}/frontend" \
  "${TEST_PROJECT}/output/overnight/state" \
  "${RUN_DIR}/browser-smoke" \
  "${RUN_DIR_REBASE}/browser-smoke" \
  "${TEST_ROOT}/bin"

cp "${PROJECT_ROOT}/scripts/overnight-frontend-followup.sh" "${TEST_PROJECT}/scripts/overnight-frontend-followup.sh"
cp "${PROJECT_ROOT}/scripts/overnight-frontend-followup-prompt.md" "${TEST_PROJECT}/scripts/overnight-frontend-followup-prompt.md"
cp "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TEST_PROJECT}/scripts/run_with_timeout.py"

cat > "${TEST_PROJECT}/scripts/feishu_notify.py" <<'EOF'
#!/usr/bin/env python3
print("notify skipped")
EOF

cat > "${TEST_PROJECT}/frontend/README.md" <<'EOF'
# test frontend
EOF

cat > "${RUN_DIR}/browser-smoke/meta.env" <<'EOF'
MODULES=resources,profile
EOF

cat > "${RUN_DIR_REBASE}/browser-smoke/meta.env" <<'EOF'
MODULES=resources
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

printf 'cwd=%s\n' "$(pwd)" > "${output_file}"
cat >> "${output_file}"
EOF

chmod +x \
  "${TEST_PROJECT}/scripts/overnight-frontend-followup.sh" \
  "${TEST_PROJECT}/scripts/run_with_timeout.py" \
  "${TEST_PROJECT}/scripts/feishu_notify.py" \
  "${TEST_ROOT}/bin/fake-codex"

git -C "${TEST_PROJECT}" init -q
git -C "${TEST_PROJECT}" config user.name "ShareHub Test"
git -C "${TEST_PROJECT}" config user.email "sharehub-test@example.com"
git -C "${TEST_PROJECT}" add scripts frontend
git -C "${TEST_PROJECT}" commit -q -m "init"
git -C "${TEST_PROJECT}" branch -M test-branch
git init --bare -q "${TEST_REMOTE}"
git -C "${TEST_PROJECT}" remote add origin "${TEST_REMOTE}"
git -C "${TEST_PROJECT}" push -q -u origin test-branch

(
  cd "${TEST_PROJECT}"
  OVERNIGHT_CODEX_BIN="${TEST_ROOT}/bin/fake-codex" \
  ./scripts/overnight-frontend-followup.sh "${RUN_DIR}" "HEAD~0" "HEAD"
) >"${TEST_ROOT}/frontend-followup.log" 2>&1 || {
  KEEP_TEST_ROOT=1
  cat "${TEST_ROOT}/frontend-followup.log" || true
  exit 1
}

PROMPT_FILE="${RUN_DIR}/frontend-followup/prompt.txt"
META_FILE="${RUN_DIR}/frontend-followup/meta.env"
LAST_MESSAGE_FILE="${RUN_DIR}/frontend-followup/last-message.md"

[[ -f "${PROMPT_FILE}" ]] || { echo "prompt file missing"; exit 1; }
[[ -f "${META_FILE}" ]] || { echo "meta file missing"; exit 1; }
[[ -f "${LAST_MESSAGE_FILE}" ]] || { echo "last message missing"; exit 1; }

grep -q '模块直达上下文' "${PROMPT_FILE}" || { echo "module context missing"; exit 1; }
grep -q 'frontend/src/views/resource/ResourceListView.vue' "${PROMPT_FILE}" || { echo "resource context missing"; exit 1; }
grep -q '^FRONTEND_WORKDIR=' "${META_FILE}" || { echo "frontend workdir missing"; exit 1; }
grep -q '/frontend$' "${LAST_MESSAGE_FILE}" || { echo "codex cwd was not frontend"; exit 1; }

git -C "${TEST_REMOTE}" show-ref --verify --quiet refs/heads/feature/frontend-real-api-resources || {
  echo "frontend branch missing on remote"
  exit 1
}

REMOTE_CLONE="${TEST_ROOT}/remote-clone"
git clone -q "${TEST_REMOTE}" "${REMOTE_CLONE}"
git -C "${REMOTE_CLONE}" config user.name "ShareHub Remote"
git -C "${REMOTE_CLONE}" config user.email "sharehub-remote@example.com"
git -C "${REMOTE_CLONE}" checkout -q -b feature/frontend-real-api-resources origin/feature/frontend-real-api-resources
echo "remote change" >> "${REMOTE_CLONE}/frontend/README.md"
git -C "${REMOTE_CLONE}" add frontend/README.md
git -C "${REMOTE_CLONE}" commit -q -m "remote update"
git -C "${REMOTE_CLONE}" push -q origin feature/frontend-real-api-resources

(
  cd "${TEST_PROJECT}"
  OVERNIGHT_CODEX_BIN="${TEST_ROOT}/bin/fake-codex" \
  ./scripts/overnight-frontend-followup.sh "${RUN_DIR_REBASE}" "HEAD~0" "HEAD"
) >"${TEST_ROOT}/frontend-followup-rebase.log" 2>&1 || {
  KEEP_TEST_ROOT=1
  cat "${TEST_ROOT}/frontend-followup-rebase.log" || true
  exit 1
}

REBASE_LOG="${RUN_DIR_REBASE}/frontend-followup/codex-output.log"
[[ -f "${REBASE_LOG}" ]] || { echo "rebase log missing"; exit 1; }
grep -q '先执行 fetch + rebase' "${REBASE_LOG}" || { echo "fetch+rebase log missing"; exit 1; }
grep -q '前端分支已完成 rebase' "${REBASE_LOG}" || { echo "rebase success log missing"; exit 1; }

UPDATED_REMOTE_CLONE="${TEST_ROOT}/remote-clone-verify"
git clone -q "${TEST_REMOTE}" "${UPDATED_REMOTE_CLONE}"
git -C "${UPDATED_REMOTE_CLONE}" checkout -q feature/frontend-real-api-resources
grep -q 'remote change' "${UPDATED_REMOTE_CLONE}/frontend/README.md" || { echo "remote change lost after rebase push"; exit 1; }

echo "frontend followup test passed"
