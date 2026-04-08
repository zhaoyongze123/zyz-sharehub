#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
PROMPT_FILE="${PROJECT_ROOT}/scripts/overnight-manager-prompt.md"
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/${RUN_ID}"
TIMEOUT_SECONDS="${OVERNIGHT_TIMEOUT_SECONDS:-3000}"
MAX_RETRIES="${OVERNIGHT_MAX_RETRIES:-3}"
PRIMARY_MODEL="${OVERNIGHT_PRIMARY_MODEL:-gpt-5.4}"
FALLBACK_MODELS="${OVERNIGHT_FALLBACK_MODELS:-gpt-5.1-codex-max,gpt-5.1-codex-mini}"
LAST_MESSAGE_FILE="${RUN_DIR}/last-message.md"
RAW_LOG_FILE="${RUN_DIR}/codex-output.log"
META_FILE="${RUN_DIR}/meta.env"
NOTIFY_SCRIPT="${PROJECT_ROOT}/scripts/feishu_notify.py"
DEFAULT_CODEX_HOME="${HOME}/.codex"
AUTOPILOT_CODEX_HOME="${OUTPUT_DIR}/codex-home"

export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:${PATH:-}"

mkdir -p "${RUN_DIR}" "${STATE_DIR}"

prepare_autopilot_codex_home() {
  mkdir -p "${AUTOPILOT_CODEX_HOME}"

  if [[ -f "${DEFAULT_CODEX_HOME}/auth.json" && ! -f "${AUTOPILOT_CODEX_HOME}/auth.json" ]]; then
    cp "${DEFAULT_CODEX_HOME}/auth.json" "${AUTOPILOT_CODEX_HOME}/auth.json"
  fi

  if [[ -f "${DEFAULT_CODEX_HOME}/config.toml" && ! -f "${AUTOPILOT_CODEX_HOME}/config.toml" ]]; then
    cp "${DEFAULT_CODEX_HOME}/config.toml" "${AUTOPILOT_CODEX_HOME}/config.toml"
  fi

  if [[ -d "${DEFAULT_CODEX_HOME}/skills" && ! -e "${AUTOPILOT_CODEX_HOME}/skills" ]]; then
    ln -s "${DEFAULT_CODEX_HOME}/skills" "${AUTOPILOT_CODEX_HOME}/skills"
  fi
}

prepare_autopilot_codex_home
export CODEX_HOME="${AUTOPILOT_CODEX_HOME}"

build_model_sequence() {
  python3 - "${PRIMARY_MODEL}" "${FALLBACK_MODELS}" <<'PY'
import sys

primary = sys.argv[1].strip()
fallbacks = [item.strip() for item in sys.argv[2].split(",") if item.strip()]
seen = set()
ordered = []
for model in [primary, *fallbacks]:
    if model and model not in seen:
        ordered.append(model)
        seen.add(model)
print("\n".join(ordered))
PY
}

MODEL_SEQUENCE=()
while IFS= read -r line; do
  if [[ -n "${line}" ]]; then
    MODEL_SEQUENCE+=("${line}")
  fi
done < <(build_model_sequence)

model_for_attempt() {
  local index=$(( $1 - 1 ))
  local last_index=$(( ${#MODEL_SEQUENCE[@]} - 1 ))
  if (( index > last_index )); then
    index=${last_index}
  fi
  printf '%s\n' "${MODEL_SEQUENCE[index]}"
}

{
  echo "RUN_ID=${RUN_ID}"
  echo "START_AT=$(date '+%F %T %z')"
  echo "PROJECT_ROOT=${PROJECT_ROOT}"
  echo "TIMEOUT_SECONDS=${TIMEOUT_SECONDS}"
  echo "MAX_RETRIES=${MAX_RETRIES}"
  echo "PRIMARY_MODEL=${PRIMARY_MODEL}"
  echo "FALLBACK_MODELS=${FALLBACK_MODELS}"
  echo "CODEX_HOME=${CODEX_HOME}"
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

echo "[$(date '+%F %T')] 开始执行第 ${RUN_ID} 轮，日志目录: ${RUN_DIR}" | tee -a "${RAW_LOG_FILE}"

TRANSIENT_ERROR_HINT=""
ATTEMPT=1
EXIT_CODE=1

while (( ATTEMPT <= MAX_RETRIES )); do
  CURRENT_MODEL="$(model_for_attempt "${ATTEMPT}")"
  CODEX_CMD=(
    codex
    exec
    --ephemeral
    --disable multi_agent
    -c 'model_reasoning_effort="low"'
    -m "${CURRENT_MODEL}"
    --dangerously-bypass-approvals-and-sandbox
    -C "${PROJECT_ROOT}"
    -o "${LAST_MESSAGE_FILE}"
  )
  rm -f "${LAST_MESSAGE_FILE}"
  echo "[$(date '+%F %T')] 执行尝试 ${ATTEMPT}/${MAX_RETRIES}，模型=${CURRENT_MODEL}" | tee -a "${RAW_LOG_FILE}"

  set +e
  python3 "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TIMEOUT_SECONDS}" "${CODEX_CMD[@]}" - \
    < "${RUN_DIR}/prompt.txt" 2>&1 | tee -a "${RAW_LOG_FILE}"
  EXIT_CODE=${PIPESTATUS[0]}
  set -e

  if [[ ${EXIT_CODE} -eq 0 && -s "${LAST_MESSAGE_FILE}" ]]; then
    break
  fi

  TRANSIENT_ERROR_HINT="$(rg -m 1 'high demand|stream disconnected|Reconnecting|temporary errors|no last agent message' "${RAW_LOG_FILE}" 2>/dev/null || true)"
  if [[ -n "${TRANSIENT_ERROR_HINT}" && ${ATTEMPT} -lt ${MAX_RETRIES} ]]; then
    BACKOFF_SECONDS=$(( ATTEMPT * 5 ))
    echo "[$(date '+%F %T')] 检测到瞬时错误，${BACKOFF_SECONDS}s 后重试: ${TRANSIENT_ERROR_HINT}" | tee -a "${RAW_LOG_FILE}"
    sleep "${BACKOFF_SECONDS}"
    ATTEMPT=$(( ATTEMPT + 1 ))
    continue
  fi

  break
done

{
  echo "END_AT=$(date '+%F %T %z')"
  echo "EXIT_CODE=${EXIT_CODE}"
  echo "ATTEMPTS=${ATTEMPT}"
  echo "LAST_MODEL=$(model_for_attempt "${ATTEMPT}")"
} >> "${META_FILE}"

git -C "${PROJECT_ROOT}" status --short --branch > "${RUN_DIR}/git-status.txt" || true
git -C "${PROJECT_ROOT}" log --oneline -n 10 > "${RUN_DIR}/git-log.txt" || true

CURRENT_BRANCH="$(git -C "${PROJECT_ROOT}" branch --show-current || true)"
PUSH_OUTPUT_FILE="${RUN_DIR}/git-push.txt"
if [[ -n "${CURRENT_BRANCH}" ]]; then
  if git -C "${PROJECT_ROOT}" push origin "${CURRENT_BRANCH}" > "${PUSH_OUTPUT_FILE}" 2>&1; then
    PUSH_STATUS="SUCCESS"
  else
    PUSH_STATUS="FAILED"
    EXIT_CODE=1
  fi
else
  PUSH_STATUS="SKIPPED"
fi

{
  echo "PUSH_STATUS=${PUSH_STATUS}"
  echo "BRANCH=${CURRENT_BRANCH}"
} >> "${META_FILE}"

cp "${LAST_MESSAGE_FILE}" "${OUTPUT_DIR}/latest-message.md" 2>/dev/null || true
cp "${RAW_LOG_FILE}" "${OUTPUT_DIR}/latest.log" 2>/dev/null || true
cp "${META_FILE}" "${OUTPUT_DIR}/latest-meta.env" 2>/dev/null || true

SUMMARY="$(tail -n 40 "${LAST_MESSAGE_FILE}" 2>/dev/null || tail -n 30 "${RAW_LOG_FILE}" | tail -n 20)"
SUMMARY="$(printf '%s' "${SUMMARY}" | tr '\n' ' ' | tr '\r' ' ' | cut -c1-1500)"
BLOCKED_LINE="$(rg -m 1 '^AUTOPILOT_BLOCKED:' "${LAST_MESSAGE_FILE}" 2>/dev/null || true)"
ISSUE_HINT="$(rg -m 1 'AUTOPILOT_BLOCKED:|ERROR|Exception|failed|timed out|Permission denied|Operation not permitted|migration' "${RAW_LOG_FILE}" 2>/dev/null || true)"
if [[ -z "${ISSUE_HINT}" && -n "${TRANSIENT_ERROR_HINT}" ]]; then
  ISSUE_HINT="${TRANSIENT_ERROR_HINT}"
fi
ISSUE_HINT="$(printf '%s' "${ISSUE_HINT}" | tr '\n' ' ' | tr '\r' ' ' | cut -c1-300)"

if [[ -n "${BLOCKED_LINE}" ]]; then
  echo "[$(date '+%F %T')] 检测到阻塞标记：${BLOCKED_LINE}" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" "ShareHub 夜间推进阻塞 | 轮次 ${RUN_ID} | 分支 ${CURRENT_BRANCH:-unknown} | ${BLOCKED_LINE}" >> "${RAW_LOG_FILE}" 2>&1 || true
  EXIT_CODE=1
fi

if [[ ${EXIT_CODE} -eq 0 ]]; then
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮完成" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" "ShareHub 夜间推进完成 | 轮次 ${RUN_ID} | 分支 ${CURRENT_BRANCH:-unknown} | push=${PUSH_STATUS} | 摘要: ${SUMMARY}" >> "${RAW_LOG_FILE}" 2>&1 || true
else
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮异常退出，exit=${EXIT_CODE}" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" "ShareHub 夜间推进异常 | 轮次 ${RUN_ID} | exit=${EXIT_CODE} | 分支 ${CURRENT_BRANCH:-unknown} | push=${PUSH_STATUS} | 问题: ${ISSUE_HINT:-未提取到关键错误，请查看 latest.log} | 摘要: ${SUMMARY}" >> "${RAW_LOG_FILE}" 2>&1 || true
fi

python3 - "${META_FILE}" "${EXIT_CODE}" "${PUSH_STATUS}" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
exit_code = sys.argv[2]
push_status = sys.argv[3]
lines = path.read_text(encoding="utf-8").splitlines()
filtered = [line for line in lines if not line.startswith("EXIT_CODE=") and not line.startswith("PUSH_STATUS=")]
filtered.append(f"EXIT_CODE={exit_code}")
filtered.append(f"PUSH_STATUS={push_status}")
path.write_text("\n".join(filtered) + "\n", encoding="utf-8")
PY

cp "${RAW_LOG_FILE}" "${OUTPUT_DIR}/latest.log" 2>/dev/null || true
cp "${META_FILE}" "${OUTPUT_DIR}/latest-meta.env" 2>/dev/null || true

exit "${EXIT_CODE}"
