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
FRONTEND_WORKDIR=""
START_EPOCH="$(date '+%s')"
TIMEOUT_SECONDS="${OVERNIGHT_FRONTEND_FOLLOWUP_TIMEOUT_SECONDS:-2400}"
MODEL="${OVERNIGHT_FRONTEND_FOLLOWUP_MODEL:-gpt-5.1-codex-max}"
CODEX_BIN="${OVERNIGHT_CODEX_BIN:-codex}"
BASE_BRANCH="$(git -C "${PROJECT_ROOT}" branch --show-current)"
FOLLOW_ALL_MODULES="${OVERNIGHT_FRONTEND_FOLLOWUP_ALL_MODULES:-0}"

mkdir -p "${FOLLOWUP_DIR}" "${WORKTREE_ROOT}"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "${RAW_LOG_FILE}"
}

extract_git_failure_reason() {
  local log_file="$1"
  python3 - <<'PY' "${log_file}"
from pathlib import Path
import sys

content = Path(sys.argv[1]).read_text(encoding="utf-8", errors="ignore")

checks = [
    ("non-fast-forward", "远端分支领先，本地推送被非快进拒绝（non-fast-forward）"),
    ("Updates were rejected because the tip of your current branch is behind", "远端分支领先，本地分支落后，需先同步后再推送"),
    ("failed to push some refs", "Git 拒绝推送 refs，需要先同步远端分支"),
    ("CONFLICT", "rebase 过程中出现真实代码冲突，需要人工处理"),
    ("could not apply", "rebase 无法自动应用补丁，存在真实代码冲突"),
]

for needle, message in checks:
    if needle in content:
        print(message)
        break
else:
    last_lines = [line.strip() for line in content.splitlines() if line.strip()][-8:]
    print("Git 操作失败，关键信息：" + " | ".join(last_lines) if last_lines else "Git 操作失败，日志中未提取到更明确原因")
PY
}

sync_frontend_branch() {
  local branch="$1"

  if git -C "${WORKTREE_DIR}" ls-remote --exit-code --heads origin "${branch}" >/dev/null 2>&1; then
    log "检测到远端前端分支已存在，先执行 fetch + rebase：${branch}"
    git -C "${WORKTREE_DIR}" fetch origin "${branch}" >> "${RAW_LOG_FILE}" 2>&1
    if ! git -C "${WORKTREE_DIR}" rebase "origin/${branch}" >> "${RAW_LOG_FILE}" 2>&1; then
      local reason
      reason="$(extract_git_failure_reason "${RAW_LOG_FILE}")"
      log "前端子代理 rebase 失败：${reason}"
      python3 "${NOTIFY_SCRIPT}" \
        --event "前端子代理异常" \
        --status "需关注" \
        --run-id "${RUN_DIR##*/}" \
        --module "${MODULES}" \
        --frontend-branch "${FRONTEND_BRANCH}" \
        --duration-seconds "${DURATION_SECONDS}" \
        --reason "前端子代理 rebase 失败：${reason}" \
        --evidence "${FOLLOWUP_DIR}/codex-output.log" \
        >> "${RAW_LOG_FILE}" 2>&1 || true
      exit 1
    fi
    log "前端分支已完成 rebase：${branch}"
  else
    log "远端前端分支不存在，首次推送无需 rebase：${branch}"
  fi
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

build_module_context() {
  python3 - <<'PY' "$1"
import sys

modules = [item.strip() for item in sys.argv[1].split(",") if item.strip()]

contexts = {
    "resources": """- 目标模块首批必读文件（只读这些开始，不要先做全仓库扫描）：
  - frontend/src/views/resource/ResourceListView.vue
  - frontend/src/views/resource/ResourceDetailView.vue
  - frontend/src/views/resource/PublishResourceView.vue
  - frontend/src/components/business/ResourceCard.vue
  - frontend/src/components/business/ResourceMeta.vue
  - frontend/src/stores/resource.ts
  - frontend/src/mock/resources.ts
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 5.1 到 5.9 资源接口段落
  - docs/openapi.yaml 中 /api/resources 相关路径
- 严禁为了找接口去大范围扫描 backend 源码；只有文档缺失且被阻塞时，才允许定点读取单个后端文件。""",
    "profile": """- 目标模块首批必读文件：
  - frontend/src/views/user/*
  - frontend/src/stores/auth.ts
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 me / profile 相关段落
  - docs/openapi.yaml 中 /api/me 相关路径""",
    "roadmaps": """- 目标模块首批必读文件：
  - frontend/src/views/roadmap/*
  - frontend/src/mock/roadmaps.ts
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 roadmap 相关段落
  - docs/openapi.yaml 中 /api/roadmaps 相关路径""",
    "notes": """- 目标模块首批必读文件：
  - frontend/src/views/note/*
  - frontend/src/mock/notes.ts
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 note 相关段落
  - docs/openapi.yaml 中 /api/notes 相关路径""",
    "resumes": """- 目标模块首批必读文件：
  - frontend/src/views/resume/*
  - frontend/src/stores/*
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 resume 相关段落
  - docs/openapi.yaml 中 /api/resumes 与 /api/me/resumes 相关路径""",
    "admin": """- 目标模块首批必读文件：
  - frontend/src/views/admin/*
  - frontend/src/api/client.ts
- 接口契约优先来源：
  - docs/backend-api-reference.md 中 admin 治理相关段落
  - docs/openapi.yaml 中 /api/admin 相关路径""",
}

parts = [contexts[module] for module in modules if module in contexts]
print("\n".join(parts))
PY
}

build_module_directive() {
  python3 - <<'PY' "$1"
import sys

modules = [item.strip() for item in sys.argv[1].split(",") if item.strip()]

if modules == ["resources"]:
    print("""- 定向执行指令：
  - `resources` 的公开读取链路已完成，本轮禁止回头重做列表页/详情页的大范围调研。
  - 本轮只允许收口一个最小任务：`src/views/resource/PublishResourceView.vue` 去掉对 `@/mock/resources` 的直接依赖。
  - 优先做法：把分类选项改为来自 `src/api/resources.ts` 的前端常量，或在不引入 mock 的前提下落到同一业务文件内。
  - 不要再花时间讨论批次归属；直接改代码、跑最小验证、提交并 push。
  - 如果 10 分钟内仍未形成代码改动，必须立即执行上述最小补丁，不允许继续调研。""")
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
FRONTEND_WORKDIR="${WORKTREE_DIR}/frontend"
MODULE_CONTEXT="$(build_module_context "${MODULES}")"
MODULE_DIRECTIVE="$(build_module_directive "${MODULES}")"

{
  cat "${PROMPT_TEMPLATE_FILE}"
  echo
  echo "自动化运行时覆盖："
  echo "- 当前任务由夜间 supervisor 直接触发，不存在人工交互环节。"
  echo "- 你必须把这条消息视为本轮唯一且充分的改动授权。"
  echo "- 禁止因为仓库中的“先展示 diff 再等待确认”规则而停住；该规则在本轮自动化中已由 supervisor 预先确认。"
  echo "- 对 frontend/ 下的最小闭环改动、验证、commit、push，必须直接执行并给出结果。"
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
  if [[ -n "${MODULE_DIRECTIVE}" ]]; then
    echo "- 定向收口要求："
    printf '%s\n' "${MODULE_DIRECTIVE}"
  fi
  if [[ -n "${MODULE_CONTEXT}" ]]; then
    echo "- 模块直达上下文："
    printf '%s\n' "${MODULE_CONTEXT}"
  fi
} > "${PROMPT_FILE}"

echo "FRONTEND_BRANCH=${FRONTEND_BRANCH}" >> "${META_FILE}"
echo "WORKTREE_DIR=${WORKTREE_DIR}" >> "${META_FILE}"
echo "FRONTEND_WORKDIR=${FRONTEND_WORKDIR}" >> "${META_FILE}"

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
  -m "${MODEL}" --dangerously-bypass-approvals-and-sandbox -C "${FRONTEND_WORKDIR}" -o "${LAST_MESSAGE_FILE}" - \
  < "${PROMPT_FILE}" 2>&1 | tee -a "${RAW_LOG_FILE}"
EXIT_CODE=${PIPESTATUS[0]}
set -e
END_EPOCH="$(date '+%s')"
DURATION_SECONDS=$(( END_EPOCH - START_EPOCH ))

if [[ ${EXIT_CODE} -ne 0 ]]; then
  log "前端子代理执行失败，exit=${EXIT_CODE}"
  python3 "${NOTIFY_SCRIPT}" \
    --event "前端子代理异常" \
    --status "需关注" \
    --run-id "${RUN_DIR##*/}" \
    --module "${MODULES}" \
    --frontend-branch "${FRONTEND_BRANCH}" \
    --duration-seconds "${DURATION_SECONDS}" \
    --reason "前端子代理执行失败，exit=${EXIT_CODE}" \
    --evidence "${FOLLOWUP_DIR}/codex-output.log" \
    >> "${RAW_LOG_FILE}" 2>&1 || true
  exit "${EXIT_CODE}"
fi

sync_frontend_branch "${FRONTEND_BRANCH}"

if ! git -C "${WORKTREE_DIR}" push origin "${FRONTEND_BRANCH}" >> "${RAW_LOG_FILE}" 2>&1; then
  PUSH_REASON="$(extract_git_failure_reason "${RAW_LOG_FILE}")"
  log "前端子代理 push 失败：${PUSH_REASON}"
  python3 "${NOTIFY_SCRIPT}" \
    --event "前端子代理异常" \
    --status "需关注" \
    --run-id "${RUN_DIR##*/}" \
    --module "${MODULES}" \
    --frontend-branch "${FRONTEND_BRANCH}" \
    --duration-seconds "${DURATION_SECONDS}" \
    --reason "前端子代理 push 失败：${PUSH_REASON}" \
    --evidence "${FOLLOWUP_DIR}/codex-output.log" \
    >> "${RAW_LOG_FILE}" 2>&1 || true
  exit 1
fi

log "前端子代理执行完成，已推送分支 ${FRONTEND_BRANCH}"
python3 "${NOTIFY_SCRIPT}" \
  --event "前端子代理完成" \
  --status "成功" \
  --run-id "${RUN_DIR##*/}" \
  --module "${MODULES}" \
  --frontend-branch "${FRONTEND_BRANCH}" \
  --duration-seconds "${DURATION_SECONDS}" \
  --result "已推送前端分支" \
  --evidence "${FOLLOWUP_DIR}/meta.env" \
  >> "${RAW_LOG_FILE}" 2>&1 || true
