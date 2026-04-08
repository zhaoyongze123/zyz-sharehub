#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUN_DIR="${1:?请传入 run 目录}"
START_HEAD="${2:-}"
END_HEAD="${3:-}"

PROMPT_TEMPLATE_FILE="${PROJECT_ROOT}/scripts/overnight-frontend-followup-prompt.md"
NOTIFY_SCRIPT="${PROJECT_ROOT}/scripts/feishu_notify.py"
FRONTEND_DIR="${PROJECT_ROOT}/frontend"
SMOKE_META_FILE="${RUN_DIR}/browser-smoke/meta.env"
FOLLOWUP_DIR="${RUN_DIR}/frontend-followup"
PROMPT_FILE="${FOLLOWUP_DIR}/prompt.txt"
LAST_MESSAGE_FILE="${FOLLOWUP_DIR}/last-message.md"
RAW_LOG_FILE="${FOLLOWUP_DIR}/codex-output.log"
META_FILE="${FOLLOWUP_DIR}/meta.env"
WORKTREE_ROOT="${PROJECT_ROOT}/output/overnight/frontend-worktrees"
TIMEOUT_SECONDS="${OVERNIGHT_FRONTEND_FOLLOWUP_TIMEOUT_SECONDS:-2400}"
MODEL="${OVERNIGHT_FRONTEND_FOLLOWUP_MODEL:-gpt-5.1-codex-max}"
CODEX_BIN="${OVERNIGHT_CODEX_BIN:-codex}"
BASE_BRANCH="$(git -C "${PROJECT_ROOT}" branch --show-current)"
FOLLOW_ALL_MODULES="${OVERNIGHT_FRONTEND_FOLLOWUP_ALL_MODULES:-0}"

mkdir -p "${FOLLOWUP_DIR}" "${WORKTREE_ROOT}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${RAW_LOG_FILE}"
}

read_meta_value() {
  local key="$1"
  local file="$2"
  awk -F'=' -v target="${key}" '$1==target{print substr($0, index($0, "=")+1); exit}' "${file}" 2>/dev/null || true
}

collect_modules() {
  local modules=""
  if [[ -f "${SMOKE_META_FILE}" ]]; then
    modules="$(read_meta_value "MODULES" "${SMOKE_META_FILE}")"
  fi

  if [[ -z "${modules}" && -n "${START_HEAD}" && -n "${END_HEAD}" ]]; then
    modules="$(python3 - <<'PY' "${PROJECT_ROOT}" "${START_HEAD}" "${END_HEAD}"
from pathlib import Path
import subprocess
import sys

project_root = Path(sys.argv[1])
start_head = sys.argv[2]
end_head = sys.argv[3]

result = subprocess.run(
    ["git", "-C", str(project_root), "diff", "--name-only", f"{start_head}..{end_head}"],
    capture_output=True,
    text=True,
    check=False,
)
files = [line.strip().lower() for line in result.stdout.splitlines() if line.strip()]
modules = []
for name in ("resources", "roadmaps", "notes", "resumes", "profile", "admin"):
    if any(name[:-1] in path or name in path for path in files):
        modules.append(name)
print(",".join(modules))
PY
)"
  fi

  python3 - <<'PY' "${modules}" "${FOLLOW_ALL_MODULES}"
import sys

raw = sys.argv[1]
follow_all = sys.argv[2] == "1"
seen = []
priority = ["resources", "profile", "roadmaps", "notes", "resumes", "admin"]
for item in raw.split(","):
    value = item.strip()
    if value == "all":
        for name in priority:
            if name not in seen:
                seen.append(name)
        continue
    if value and value not in {"backend", "public"} and value not in seen:
        seen.append(value)
if not follow_all and seen:
    ordered = [name for name in priority if name in seen]
    if ordered:
        seen = [ordered[0]]
    else:
        seen = [seen[0]]
print(",".join(seen))
PY
}

sanitize_branch_suffix() {
  python3 - <<'PY' "$1"
import re
import sys

value = sys.argv[1].strip().lower()
value = re.sub(r"[^a-z0-9]+", "-", value).strip("-")
print((value or "frontend-followup")[:48])
PY
}

MODULES="$(collect_modules)"

{
  echo "BASE_BRANCH=${BASE_BRANCH}"
  echo "MODULES=${MODULES}"
  echo "MODEL=${MODEL}"
  echo "TIMEOUT_SECONDS=${TIMEOUT_SECONDS}"
  echo "FOLLOW_ALL_MODULES=${FOLLOW_ALL_MODULES}"
} > "${META_FILE}"

if [[ -z "${MODULES}" ]]; then
  log "未识别到可跟进的前端模块，跳过前端子代理"
  echo "FRONTEND_BRANCH=SKIPPED" >> "${META_FILE}"
  exit 0
fi

BRANCH_SUFFIX="$(sanitize_branch_suffix "${MODULES}")"
FRONTEND_BRANCH="feature/frontend-real-api-${BRANCH_SUFFIX}"
WORKTREE_DIR="${WORKTREE_ROOT}/${FRONTEND_BRANCH//\//-}-${RUN_DIR##*/}"

{
  cat "${PROMPT_TEMPLATE_FILE}"
  echo
  echo "补充上下文："
  echo "- 本轮通过联调模块：${MODULES}"
  echo "- 源分支：${BASE_BRANCH}"
  echo "- 当前前端分支：${FRONTEND_BRANCH}"
  echo "- 前端目录：${FRONTEND_DIR}"
  echo "- 工作约束："
  echo "  - 只修改 frontend/ 下文件"
  echo "  - 完成后提交并 push 到 ${FRONTEND_BRANCH}"
  echo "  - 尽量优先 resources、profile、roadmaps；如果本轮模块不是这三个，也按实际模块处理"
} > "${PROMPT_FILE}"

echo "FRONTEND_BRANCH=${FRONTEND_BRANCH}" >> "${META_FILE}"
echo "WORKTREE_DIR=${WORKTREE_DIR}" >> "${META_FILE}"

if [[ "${OVERNIGHT_FRONTEND_FOLLOWUP_DRY_RUN:-0}" == "1" ]]; then
  log "前端子代理 dry-run 完成，模块=${MODULES}，分支=${FRONTEND_BRANCH}"
  exit 0
fi

cleanup() {
  if git -C "${PROJECT_ROOT}" worktree list | grep -Fq "${WORKTREE_DIR}"; then
    git -C "${PROJECT_ROOT}" worktree remove --force "${WORKTREE_DIR}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

git -C "${PROJECT_ROOT}" worktree add -B "${FRONTEND_BRANCH}" "${WORKTREE_DIR}" "${BASE_BRANCH}" >/dev/null

log "前端子代理工作树已创建，分支=${FRONTEND_BRANCH}"

set +e
python3 "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TIMEOUT_SECONDS}" \
  "${CODEX_BIN}" exec --ephemeral --disable multi_agent -c 'model_reasoning_effort="medium"' \
  -m "${MODEL}" --dangerously-bypass-approvals-and-sandbox -C "${WORKTREE_DIR}" -o "${LAST_MESSAGE_FILE}" - \
  < "${PROMPT_FILE}" 2>&1 | tee -a "${RAW_LOG_FILE}"
EXIT_CODE=${PIPESTATUS[0]}
set -e

if [[ ${EXIT_CODE} -ne 0 ]]; then
  log "前端子代理执行失败，exit=${EXIT_CODE}"
  python3 "${NOTIFY_SCRIPT}" "ShareHub 前端子代理异常 | 轮次 ${RUN_DIR##*/} | 模块 ${MODULES} | 分支 ${FRONTEND_BRANCH} | exit=${EXIT_CODE}" >> "${RAW_LOG_FILE}" 2>&1 || true
  exit "${EXIT_CODE}"
fi

if ! git -C "${WORKTREE_DIR}" push origin "${FRONTEND_BRANCH}" >> "${RAW_LOG_FILE}" 2>&1; then
  log "前端子代理 push 失败"
  python3 "${NOTIFY_SCRIPT}" "ShareHub 前端子代理异常 | 轮次 ${RUN_DIR##*/} | 模块 ${MODULES} | 分支 ${FRONTEND_BRANCH} | push 失败" >> "${RAW_LOG_FILE}" 2>&1 || true
  exit 1
fi

log "前端子代理执行完成，已推送分支 ${FRONTEND_BRANCH}"
python3 "${NOTIFY_SCRIPT}" "ShareHub 前端子代理完成 | 轮次 ${RUN_DIR##*/} | 模块 ${MODULES} | 分支 ${FRONTEND_BRANCH}" >> "${RAW_LOG_FILE}" 2>&1 || true
