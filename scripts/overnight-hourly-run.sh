#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="${OVERNIGHT_PROJECT_ROOT:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
source "${PROJECT_ROOT}/scripts/load-env.sh"
load_env_stack "${PROJECT_ROOT}/scripts" ".env.overnight" ".env.overnight.local"
OUTPUT_DIR="${PROJECT_ROOT}/output/overnight"
STATE_DIR="${OUTPUT_DIR}/state"
RUN_LOCK_DIR="${STATE_DIR}/hourly-run.lock"
RUN_LOCK_META_FILE="${RUN_LOCK_DIR}/owner.env"
SNAPSHOT_SCRIPT="${STATE_DIR}/hourly-run.exec.sh"
PROMPT_FILE="${PROJECT_ROOT}/scripts/overnight-manager-prompt.md"
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/${RUN_ID}"
TIMEOUT_SECONDS="${OVERNIGHT_TIMEOUT_SECONDS:-3000}"
MAX_RETRIES="${OVERNIGHT_MAX_RETRIES:-3}"
PRIMARY_MODEL="${OVERNIGHT_PRIMARY_MODEL:-gpt-5.4}"
FALLBACK_MODELS="${OVERNIGHT_FALLBACK_MODELS:-gpt-5.1-codex-max,gpt-5.1-codex-mini}"
CODEX_BIN="${OVERNIGHT_CODEX_BIN:-codex}"
ENABLE_MULTI_AGENT="${OVERNIGHT_ENABLE_MULTI_AGENT:-1}"
ADMIN_AUTOPILOT="${OVERNIGHT_ADMIN_AUTOPILOT:-1}"
ADMIN_PARALLEL_MODE="${OVERNIGHT_ADMIN_PARALLEL_MODE:-2way}"
ADMIN_REQUIRE_POSTGRES="${OVERNIGHT_ADMIN_REQUIRE_POSTGRES:-1}"
POST_RUN_SMOKE_SCRIPT="${PROJECT_ROOT}/scripts/overnight-browser-smoke.sh"
POST_FRONTEND_FOLLOWUP_SCRIPT="${PROJECT_ROOT}/scripts/overnight-frontend-followup.sh"
LAST_MESSAGE_FILE="${RUN_DIR}/last-message.md"
RAW_LOG_FILE="${RUN_DIR}/codex-output.log"
META_FILE="${RUN_DIR}/meta.env"
NOTIFY_SCRIPT="${PROJECT_ROOT}/scripts/feishu_notify.py"
DEFAULT_CODEX_HOME="${HOME}/.codex"
AUTOPILOT_CODEX_HOME="${OUTPUT_DIR}/codex-home"
START_HEAD="$(git -C "${PROJECT_ROOT}" rev-parse HEAD)"
RUN_SOURCE="${OVERNIGHT_RUN_SOURCE:-manual}"
START_EPOCH="$(date '+%s')"
AUTH_LINE_MESSAGE_FILE="${RUN_DIR}/auth-line-message.md"
GATE_LINE_MESSAGE_FILE="${RUN_DIR}/gate-line-message.md"
AUTH_LINE_LOG_FILE="${RUN_DIR}/auth-line.log"
GATE_LINE_LOG_FILE="${RUN_DIR}/gate-line.log"
AUTH_LINE_PROMPT_FILE="${RUN_DIR}/auth-line-prompt.txt"
GATE_LINE_PROMPT_FILE="${RUN_DIR}/gate-line-prompt.txt"
ADMIN_AUTH_EXIT_CODE="SKIPPED"
ADMIN_SMOKE_EXIT_CODE="SKIPPED"
ADMIN_GATE_EXIT_CODE="SKIPPED"

export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:${PATH:-}"

mkdir -p "${RUN_DIR}" "${STATE_DIR}"
chmod +x "${PROJECT_ROOT}/scripts/overnight-completion-check.py" 2>/dev/null || true

if [[ "${OVERNIGHT_RUN_SNAPSHOT_ACTIVE:-0}" != "1" ]]; then
  cp "$0" "${SNAPSHOT_SCRIPT}"
  chmod +x "${SNAPSHOT_SCRIPT}"
  exec env \
    OVERNIGHT_RUN_SNAPSHOT_ACTIVE=1 \
    OVERNIGHT_PROJECT_ROOT="${PROJECT_ROOT}" \
    OVERNIGHT_RUN_SOURCE="${RUN_SOURCE}" \
    bash "${SNAPSHOT_SCRIPT}" "$@"
fi

write_run_lock() {
  cat > "${RUN_LOCK_META_FILE}" <<EOF
PID=$$
RUN_ID=${RUN_ID}
SOURCE=${RUN_SOURCE}
START_AT=$(date '+%F %T %z')
EOF
}

release_run_lock() {
  if [[ ! -f "${RUN_LOCK_META_FILE}" ]]; then
    return
  fi

  local owner_pid
  owner_pid="$(awk -F'=' '$1=="PID"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"
  if [[ "${owner_pid}" == "$$" ]]; then
    rm -rf "${RUN_LOCK_DIR}"
  fi
}

acquire_run_lock() {
  if mkdir "${RUN_LOCK_DIR}" 2>/dev/null; then
    write_run_lock
    return 0
  fi

  local owner_pid owner_run_id owner_source
  owner_pid="$(awk -F'=' '$1=="PID"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"
  owner_run_id="$(awk -F'=' '$1=="RUN_ID"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"
  owner_source="$(awk -F'=' '$1=="SOURCE"{print $2; exit}' "${RUN_LOCK_META_FILE}" 2>/dev/null || true)"

  if [[ -n "${owner_pid}" ]] && kill -0 "${owner_pid}" >/dev/null 2>&1; then
    echo "[$(date '+%F %T')] 检测到已有轮次在运行，PID=${owner_pid}，RUN_ID=${owner_run_id:-unknown}，SOURCE=${owner_source:-unknown}，跳过本次启动"
    exit 0
  fi

  rm -rf "${RUN_LOCK_DIR}"
  mkdir "${RUN_LOCK_DIR}"
  write_run_lock
}

trap release_run_lock EXIT
acquire_run_lock

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
  echo "ADMIN_AUTOPILOT=${ADMIN_AUTOPILOT}"
  echo "ADMIN_PARALLEL_MODE=${ADMIN_PARALLEL_MODE}"
  echo "ADMIN_REQUIRE_POSTGRES=${ADMIN_REQUIRE_POSTGRES}"
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

cat "${RUN_DIR}/prompt.txt" > "${AUTH_LINE_PROMPT_FILE}"
cat >> "${AUTH_LINE_PROMPT_FILE}" <<'EOF'

## 本轮固定 ownership
- 当前工作线：线 A（后台鉴权与安全收口）
- 仅允许修改：后台鉴权、安全配置、管理员白名单、`/api/auth/me`、后台权限相关测试
- 禁止修改：后台页面、运行文档、完成判定脚本、browser smoke 脚本
EOF

cat "${RUN_DIR}/prompt.txt" > "${GATE_LINE_PROMPT_FILE}"
cat >> "${GATE_LINE_PROMPT_FILE}" <<'EOF'

## 本轮固定 ownership
- 当前工作线：线 B（后台功能与可用性门禁）
- 仅允许修改：后台页面、后台 smoke / E2E、完成判定脚本、运行文档、门禁脚本
- 禁止修改：核心后台鉴权过滤器、管理员白名单表结构
EOF

echo "[$(date '+%F %T')] 开始执行第 ${RUN_ID} 轮，日志目录: ${RUN_DIR}" | tee -a "${RAW_LOG_FILE}"

TRANSIENT_ERROR_HINT=""
ATTEMPT=1
EXIT_CODE=1

run_codex_attempt() {
  local prompt_file="$1"
  local output_file="$2"
  local log_file="$3"
  local model="$4"

  local cmd=(
    "${CODEX_BIN}"
    exec
    --ephemeral
    -c 'model_reasoning_effort="low"'
    -m "${model}"
    --dangerously-bypass-approvals-and-sandbox
    -C "${PROJECT_ROOT}"
    -o "${output_file}"
  )
  if [[ "${ENABLE_MULTI_AGENT}" != "1" ]]; then
    cmd+=(--disable multi_agent)
  fi

  python3 "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TIMEOUT_SECONDS}" "${cmd[@]}" - < "${prompt_file}" > "${log_file}" 2>&1
}

while (( ATTEMPT <= MAX_RETRIES )); do
  CURRENT_MODEL="$(model_for_attempt "${ATTEMPT}")"
  rm -f "${LAST_MESSAGE_FILE}"
  echo "[$(date '+%F %T')] 执行尝试 ${ATTEMPT}/${MAX_RETRIES}，模型=${CURRENT_MODEL}" | tee -a "${RAW_LOG_FILE}"

  if [[ "${ADMIN_AUTOPILOT}" == "1" && "${ADMIN_PARALLEL_MODE}" == "2way" ]]; then
    echo "[$(date '+%F %T')] 后台专项模式：启动两条工作线并行执行" | tee -a "${RAW_LOG_FILE}"
    rm -f "${AUTH_LINE_MESSAGE_FILE}" "${GATE_LINE_MESSAGE_FILE}" "${AUTH_LINE_LOG_FILE}" "${GATE_LINE_LOG_FILE}"

    set +e
    run_codex_attempt "${AUTH_LINE_PROMPT_FILE}" "${AUTH_LINE_MESSAGE_FILE}" "${AUTH_LINE_LOG_FILE}" "${CURRENT_MODEL}" &
    AUTH_PID=$!
    run_codex_attempt "${GATE_LINE_PROMPT_FILE}" "${GATE_LINE_MESSAGE_FILE}" "${GATE_LINE_LOG_FILE}" "${CURRENT_MODEL}" &
    GATE_PID=$!
    wait "${AUTH_PID}"
    AUTH_LINE_EXIT_CODE=$?
    wait "${GATE_PID}"
    GATE_LINE_EXIT_CODE=$?
    set -e

    cat "${AUTH_LINE_LOG_FILE}" >> "${RAW_LOG_FILE}" 2>/dev/null || true
    cat "${GATE_LINE_LOG_FILE}" >> "${RAW_LOG_FILE}" 2>/dev/null || true

    ADMIN_AUTH_EXIT_CODE="${AUTH_LINE_EXIT_CODE}"
    if [[ ${AUTH_LINE_EXIT_CODE} -eq 0 && ${GATE_LINE_EXIT_CODE} -eq 0 && -s "${AUTH_LINE_MESSAGE_FILE}" && -s "${GATE_LINE_MESSAGE_FILE}" ]]; then
      {
        echo "# 线 A 结果"
        cat "${AUTH_LINE_MESSAGE_FILE}"
        echo
        echo "# 线 B 结果"
        cat "${GATE_LINE_MESSAGE_FILE}"
      } > "${LAST_MESSAGE_FILE}"
      EXIT_CODE=0
      break
    fi

    EXIT_CODE=1
    TRANSIENT_ERROR_HINT="$(rg -m 1 'high demand|stream disconnected|Reconnecting|temporary errors|no last agent message' "${RAW_LOG_FILE}" 2>/dev/null || true)"
  else
    CODEX_CMD=(
      "${CODEX_BIN}"
      exec
      --ephemeral
      -c 'model_reasoning_effort="low"'
      -m "${CURRENT_MODEL}"
      --dangerously-bypass-approvals-and-sandbox
      -C "${PROJECT_ROOT}"
      -o "${LAST_MESSAGE_FILE}"
    )
    if [[ "${ENABLE_MULTI_AGENT}" != "1" ]]; then
      CODEX_CMD+=(--disable multi_agent)
    fi

    set +e
    python3 "${PROJECT_ROOT}/scripts/run_with_timeout.py" "${TIMEOUT_SECONDS}" "${CODEX_CMD[@]}" - \
      < "${RUN_DIR}/prompt.txt" 2>&1 | tee -a "${RAW_LOG_FILE}"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e

    if [[ ${EXIT_CODE} -eq 0 && -s "${LAST_MESSAGE_FILE}" ]]; then
      break
    fi

    TRANSIENT_ERROR_HINT="$(rg -m 1 'high demand|stream disconnected|Reconnecting|temporary errors|no last agent message' "${RAW_LOG_FILE}" 2>/dev/null || true)"
  fi

  if [[ -n "${TRANSIENT_ERROR_HINT}" && ${ATTEMPT} -lt ${MAX_RETRIES} ]]; then
    BACKOFF_SECONDS=$(( ATTEMPT * 5 ))
    echo "[$(date '+%F %T')] 检测到瞬时错误，${BACKOFF_SECONDS}s 后重试: ${TRANSIENT_ERROR_HINT}" | tee -a "${RAW_LOG_FILE}"
    sleep "${BACKOFF_SECONDS}"
    ATTEMPT=$(( ATTEMPT + 1 ))
    continue
  fi

  break
done

END_HEAD="$(git -C "${PROJECT_ROOT}" rev-parse HEAD)"

if [[ ${EXIT_CODE} -eq 0 && -s "${LAST_MESSAGE_FILE}" ]]; then
  echo "[$(date '+%F %T')] 开始执行前后端浏览器联调 smoke" | tee -a "${RAW_LOG_FILE}"
  set +e
  env \
    OVERNIGHT_ADMIN_AUTOPILOT="${ADMIN_AUTOPILOT}" \
    OVERNIGHT_ADMIN_REQUIRE_POSTGRES="${ADMIN_REQUIRE_POSTGRES}" \
    bash "${POST_RUN_SMOKE_SCRIPT}" "${RUN_DIR}" "${START_HEAD}" "${END_HEAD}" 2>&1 | tee -a "${RAW_LOG_FILE}"
  SMOKE_EXIT_CODE=${PIPESTATUS[0]}
  set -e
  echo "SMOKE_EXIT_CODE=${SMOKE_EXIT_CODE}" >> "${META_FILE}"
  if [[ -f "${RUN_DIR}/browser-smoke/meta.env" ]]; then
    ADMIN_SMOKE_EXIT_CODE="$(awk -F'=' '$1=="ADMIN_SMOKE_EXIT_CODE"{print $2; exit}' "${RUN_DIR}/browser-smoke/meta.env" 2>/dev/null || echo "${SMOKE_EXIT_CODE}")"
    ADMIN_GATE_EXIT_CODE="$(awk -F'=' '$1=="ADMIN_GATE_EXIT_CODE"{print $2; exit}' "${RUN_DIR}/browser-smoke/meta.env" 2>/dev/null || echo "${SMOKE_EXIT_CODE}")"
  else
    ADMIN_SMOKE_EXIT_CODE="${SMOKE_EXIT_CODE}"
    ADMIN_GATE_EXIT_CODE="${SMOKE_EXIT_CODE}"
  fi
  if [[ ${SMOKE_EXIT_CODE} -ne 0 ]]; then
    EXIT_CODE=${SMOKE_EXIT_CODE}
  fi
else
  echo "SMOKE_EXIT_CODE=SKIPPED" >> "${META_FILE}"
fi

echo "[$(date '+%F %T')] 后台专项状态码：ADMIN_AUTH_EXIT_CODE=${ADMIN_AUTH_EXIT_CODE} ADMIN_SMOKE_EXIT_CODE=${ADMIN_SMOKE_EXIT_CODE} ADMIN_GATE_EXIT_CODE=${ADMIN_GATE_EXIT_CODE}" | tee -a "${RAW_LOG_FILE}"

if [[ ${EXIT_CODE} -eq 0 && -s "${LAST_MESSAGE_FILE}" && "${ADMIN_AUTOPILOT}" != "1" ]]; then
  echo "[$(date '+%F %T')] 开始执行前端跟进子代理" | tee -a "${RAW_LOG_FILE}"
  set +e
  bash "${POST_FRONTEND_FOLLOWUP_SCRIPT}" "${RUN_DIR}" "${START_HEAD}" "${END_HEAD}" 2>&1 | tee -a "${RAW_LOG_FILE}"
  FRONTEND_FOLLOWUP_EXIT_CODE=${PIPESTATUS[0]}
  set -e
  echo "FRONTEND_FOLLOWUP_EXIT_CODE=${FRONTEND_FOLLOWUP_EXIT_CODE}" >> "${META_FILE}"
  if [[ ${FRONTEND_FOLLOWUP_EXIT_CODE} -ne 0 ]]; then
    EXIT_CODE=${FRONTEND_FOLLOWUP_EXIT_CODE}
  fi
else
  if [[ "${ADMIN_AUTOPILOT}" == "1" ]]; then
    echo "[$(date '+%F %T')] 后台专项模式已启用，跳过公开站点前端跟进子代理" | tee -a "${RAW_LOG_FILE}"
  fi
  echo "FRONTEND_FOLLOWUP_EXIT_CODE=SKIPPED" >> "${META_FILE}"
fi

{
  echo "END_AT=$(date '+%F %T %z')"
  echo "EXIT_CODE=${EXIT_CODE}"
  echo "ATTEMPTS=${ATTEMPT}"
  echo "LAST_MODEL=$(model_for_attempt "${ATTEMPT}")"
  echo "ADMIN_AUTH_EXIT_CODE=${ADMIN_AUTH_EXIT_CODE}"
  echo "ADMIN_SMOKE_EXIT_CODE=${ADMIN_SMOKE_EXIT_CODE}"
  echo "ADMIN_GATE_EXIT_CODE=${ADMIN_GATE_EXIT_CODE}"
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
ISSUE_HINT="$(rg -m 1 '^ERROR:|unexpected status|stream disconnected|timed out|Permission denied|Operation not permitted|no last agent message' "${RAW_LOG_FILE}" 2>/dev/null || true)"
if [[ -z "${ISSUE_HINT}" && -n "${TRANSIENT_ERROR_HINT}" ]]; then
  ISSUE_HINT="${TRANSIENT_ERROR_HINT}"
fi
ISSUE_HINT="$(printf '%s' "${ISSUE_HINT}" | tr '\n' ' ' | tr '\r' ' ' | cut -c1-300)"
END_EPOCH="$(date '+%s')"
DURATION_SECONDS=$(( END_EPOCH - START_EPOCH ))

NEXT_TASK_STATUS="未启用自动续跑（手动触发）"
if [[ "${RUN_SOURCE}" == "supervisor" ]]; then
  NEXT_TASK_STATUS="由 supervisor 自动续跑"
fi

SMOKE_STATUS_TEXT="跳过"
if [[ "${SMOKE_EXIT_CODE:-SKIPPED}" == "0" ]]; then
  SMOKE_STATUS_TEXT="通过"
elif [[ "${SMOKE_EXIT_CODE:-SKIPPED}" != "SKIPPED" ]]; then
  SMOKE_STATUS_TEXT="失败"
fi

FRONTEND_FOLLOWUP_STATUS_TEXT="跳过"
if [[ "${FRONTEND_FOLLOWUP_EXIT_CODE:-SKIPPED}" == "0" ]]; then
  FRONTEND_FOLLOWUP_STATUS_TEXT="通过"
elif [[ "${FRONTEND_FOLLOWUP_EXIT_CODE:-SKIPPED}" != "SKIPPED" ]]; then
  FRONTEND_FOLLOWUP_STATUS_TEXT="失败"
fi

if [[ -n "${BLOCKED_LINE}" ]]; then
  echo "[$(date '+%F %T')] 检测到阻塞标记：${BLOCKED_LINE}" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" \
    --event "自动推进阻塞" \
    --status "需关注" \
    --run-id "${RUN_ID}" \
    --branch "${CURRENT_BRANCH:-unknown}" \
    --duration-seconds "${DURATION_SECONDS}" \
    --stage "主轮次" \
    --reason "${BLOCKED_LINE}" \
    --impact "本轮未能继续推进，需要查看日志排查" \
    --evidence "${RUN_DIR}/codex-output.log" \
    >> "${RAW_LOG_FILE}" 2>&1 || true
  EXIT_CODE=1
fi

if [[ ${EXIT_CODE} -eq 0 ]]; then
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮完成" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" \
    --event "单轮完成" \
    --status "成功" \
    --run-id "${RUN_ID}" \
    --branch "${CURRENT_BRANCH:-unknown}" \
    --duration-seconds "${DURATION_SECONDS}" \
    --commit "$(git -C "${PROJECT_ROOT}" log --oneline -1 2>/dev/null || true)" \
    --smoke "${SMOKE_STATUS_TEXT}" \
    --frontend-followup "${FRONTEND_FOLLOWUP_STATUS_TEXT}" \
    --push-status "${PUSH_STATUS}" \
    --next-task "${NEXT_TASK_STATUS}" \
    --result "${SUMMARY}" \
    --evidence "${RUN_DIR}/meta.env" \
    >> "${RAW_LOG_FILE}" 2>&1 || true
else
  echo "[$(date '+%F %T')] 第 ${RUN_ID} 轮异常退出，exit=${EXIT_CODE}" | tee -a "${RAW_LOG_FILE}"
  python3 "${NOTIFY_SCRIPT}" \
    --event "单轮异常" \
    --status "需关注" \
    --run-id "${RUN_ID}" \
    --branch "${CURRENT_BRANCH:-unknown}" \
    --duration-seconds "${DURATION_SECONDS}" \
    --smoke "${SMOKE_STATUS_TEXT}" \
    --frontend-followup "${FRONTEND_FOLLOWUP_STATUS_TEXT}" \
    --push-status "${PUSH_STATUS}" \
    --next-task "${NEXT_TASK_STATUS}" \
    --reason "${ISSUE_HINT:-未提取到关键错误，请查看 latest.log}" \
    --result "${SUMMARY}" \
    --evidence "${RUN_DIR}/codex-output.log" \
    >> "${RAW_LOG_FILE}" 2>&1 || true
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
