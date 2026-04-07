#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
PROMPT_FILE="${PROJECT_ROOT}/scripts/overnight-manager-prompt.md"
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/${RUN_ID}"
TIMEOUT_SECONDS="${OVERNIGHT_TIMEOUT_SECONDS:-3000}"
LAST_MESSAGE_FILE="${RUN_DIR}/last-message.md"
RAW_LOG_FILE="${RUN_DIR}/codex-output.log"
META_FILE="${RUN_DIR}/meta.env"
USE_RESUME_MARKER="${STATE_DIR}/use_resume"

mkdir -p "${RUN_DIR}" "${STATE_DIR}"

{
  echo "RUN_ID=${RUN_ID}"
  echo "START_AT=$(date '+%F %T %z')"
  echo "PROJECT_ROOT=${PROJECT_ROOT}"
  echo "TIMEOUT_SECONDS=${TIMEOUT_SECONDS}"
} > "${META_FILE}"

{
  echo "# 夜间自动推进"
  echo
  echo "- 轮次: ${RUN_ID}"
  echo "- 开始时间: $(date '+%F %T %z')"
  echo "- 当前分支: $(git -C "${PROJECT_ROOT}" branch --show-current)"
  echo
  echo "## Git 状态"
  git -C "${PROJECT_ROOT}" status --short --branch || true
  echo
  echo "## 最近提交"
  git -C "${PROJECT_ROOT}" log --oneline -n 10 || true
  echo
  echo "## 任务指令"
  cat "${PROMPT_FILE}"
} > "${RUN_DIR}/prompt.txt"

CODEX_BASE_CMD=(
  codex
  exec
  --dangerously-bypass-approvals-and-sandbox
  -C "${PROJECT_ROOT}"
  -o "${LAST_MESSAGE_FILE}"
)

if [[ -f "${USE_RESUME_MARKER}" ]]; then
  CODEX_CMD=("${CODEX_BASE_CMD[@]}" resume --last)
else
  CODEX_CMD=("${CODEX_BASE_CMD[@]}")
fi

echo "[$(date '+%F %T')] 开始执行第 ${RUN_ID} 轮，日志目录: ${RUN_DIR}" | tee -a "${RAW_LOG_FILE}"

set +e
python3 "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TIMEOUT_SECONDS}" \
  "${CODEX_CMD[@]}" - < "${RUN_DIR}/prompt.txt" \
  2>&1 | tee -a "${RAW_LOG_FILE}"
EXIT_CODE=${PIPESTATUS[0]}
set -e

touch "${USE_RESUME_MARKER}"

{
  echo "END_AT=$(date '+%F %T %z')"
  echo "EXIT_CODE=${EXIT_CODE}"
} >> "${META_FILE}"

git -C "${PROJECT_ROOT}" status --short --branch > "${RUN_DIR}/git-status.txt" || true
git -C "${PROJECT_ROOT}" log --oneline -n 10 > "${RUN_DIR}/git-log.txt" || true

cp "${LAST_MESSAGE_FILE}" "${OUTPUT_DIR}/latest-message.md" 2>/dev/null || true
cp "${RAW_LOG_FILE}" "${OUTPUT_DIR}/latest.log" 2>/dev/null || true
cp "${META_FILE}" "${OUTPUT_DIR}/latest-meta.env" 2>/dev/null || true

if [[ ${EXIT_CODE} -eq 0 ]]; then
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮完成" | tee -a "${RAW_LOG_FILE}"
else
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮异常退出，exit=${EXIT_CODE}" | tee -a "${RAW_LOG_FILE}"
fi

exit "${EXIT_CODE}"
