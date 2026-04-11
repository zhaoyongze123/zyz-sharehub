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
mkdir -p \
  "${TEST_PROJECT}/scripts" \
  "${TEST_PROJECT}/frontend/tests/e2e" \
  "${TEST_PROJECT}/docs" \
  "${TEST_PROJECT}/backend/src/main/resources" \
  "${TEST_PROJECT}/backend/src/test/java/com/sharehub/config" \
  "${TEST_PROJECT}/deploy" \
  "${TEST_PROJECT}/output/overnight/20260412-000000/browser-smoke"

cp "${PROJECT_ROOT}/scripts/overnight-completion-check.py" "${TEST_PROJECT}/scripts/overnight-completion-check.py"
chmod +x "${TEST_PROJECT}/scripts/overnight-completion-check.py"

cat > "${TEST_PROJECT}/scripts/overnight-manager-prompt.md" <<'EOF'
你是 ShareHub 项目的夜间值守经理 agent。

目标：
- 只推进后台管理生产级改造专项，不再推进公开站点、资源广场、路线广场、笔记详情、发布页或全站走查。
EOF

cat > "${TEST_PROJECT}/scripts/overnight-hourly-run.sh" <<'EOF'
#!/usr/bin/env bash
echo "ADMIN_AUTH_EXIT_CODE"
echo "ADMIN_SMOKE_EXIT_CODE"
echo "ADMIN_GATE_EXIT_CODE"
echo "ADMIN_SMOKE_SCRIPT_EXIT_CODE"
echo "已禁用公开站点前端跟进子代理"
EOF

cat > "${TEST_PROJECT}/scripts/overnight-browser-smoke.sh" <<'EOF'
#!/usr/bin/env bash
export OVERNIGHT_ADMIN_AUTOPILOT=1
export PLAYWRIGHT_ADMIN_USER_KEY=playwright-admin
export PLAYWRIGHT_MODULES="${PLAYWRIGHT_MODULES:-admin,backend}"
echo "validate_admin_modules"
echo "后台专项 smoke 仅允许 admin/backend 模块"
echo "validate_admin_scope"
echo "后台 smoke 缺少后台页面验收路由"
echo "ADMIN_SMOKE_SCRIPT_EXIT_CODE"
echo "/actuator/health/readiness"
echo "/actuator/health/liveness"
echo 'record_failure "${label} 未返回 status=UP"'
echo 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health"'
echo 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health/readiness"'
echo 'check_health_status_up "${BACKEND_BASE_URL}/actuator/health/liveness"'
echo "application.yml 未将 dev token 默认设为关闭且仅允许显式开启"
echo "application-cloud-dev.yml 未将 dev token 默认设为关闭且仅允许显式开启"
echo "application-test.yml 未将 dev token 默认设为关闭且仅允许显式开启"
echo "生产环境错误接受了 X-Admin-Token"
echo "缺少生产禁用 dev token 集成测试配置"
echo "缺少生产禁用 dev token 集成测试断言"
echo "缺少本地显式开启 dev token 集成测试配置"
echo "缺少本地显式开启 dev token 集成测试断言"
echo "生产部署仍然包含 MySQL 配置"
echo "生产部署未显式使用 PostgreSQL JDBC，未满足 PostgreSQL-only"
EOF

cat > "${TEST_PROJECT}/frontend/tests/e2e/admin-smoke.spec.ts" <<'EOF'
test('后台专项 smoke', async ({ page }) => {
  await page.goto('/admin')
  await page.goto('/admin/reports')
  await page.goto('/admin/reviews')
  await page.goto('/admin/audit-logs')
  await page.goto('/admin/users')
  await browserFetch(page, '/api/admin/reports?page=1&pageSize=20', {
    'X-User-Key': 'playwright-admin'
  })
})
const PLAYWRIGHT_ADMIN_USER_KEY = 'playwright-admin'
EOF

cat > "${TEST_PROJECT}/docs/admin-automation-runbook.md" <<'EOF'
ADMIN_AUTH_EXIT_CODE
ADMIN_SMOKE_EXIT_CODE
ADMIN_GATE_EXIT_CODE
EOF

cat > "${TEST_PROJECT}/docs/admin-release-gates.md" <<'EOF'
PostgreSQL-only
readiness / liveness
EOF

cat > "${TEST_PROJECT}/docs/admin-rollback-checklist.md" <<'EOF'
git checkout <上一稳定提交>
ADMIN_GATE_EXIT_CODE
EOF

cat > "${TEST_PROJECT}/backend/src/main/resources/application.yml" <<'EOF'
dev-token-enabled: ${SHAREHUB_ADMIN_DEV_TOKEN_ENABLED:false}
jdbc:postgresql://localhost:5432/sharehub
EOF

cat > "${TEST_PROJECT}/backend/src/main/resources/application-cloud-dev.yml" <<'EOF'
dev-token-enabled: ${SHAREHUB_ADMIN_DEV_TOKEN_ENABLED:false}
EOF

cat > "${TEST_PROJECT}/backend/src/main/resources/application-test.yml" <<'EOF'
dev-token-enabled: false
EOF

cat > "${TEST_PROJECT}/backend/src/test/java/com/sharehub/config/AdminTokenFilterProdModeIntegrationTest.java" <<'EOF'
sharehub.admin.dev-token-enabled=false
shouldRejectAdminTokenWhenDevTokenModeDisabled
EOF

cat > "${TEST_PROJECT}/backend/src/test/java/com/sharehub/config/AdminTokenFilterDevModeIntegrationTest.java" <<'EOF'
sharehub.admin.dev-token-enabled=true
shouldAllowAdminEndpointsViaDevTokenWhenExplicitlyEnabled
EOF

cat > "${TEST_PROJECT}/deploy/docker-compose.prod.yml" <<'EOF'
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/sharehub
EOF

cat > "${TEST_PROJECT}/output/overnight/latest-meta.env" <<'EOF'
RUN_ID=20260412-000000
ADMIN_AUTH_EXIT_CODE=0
ADMIN_SMOKE_EXIT_CODE=0
ADMIN_GATE_EXIT_CODE=0
EOF

cat > "${TEST_PROJECT}/output/overnight/20260412-000000/browser-smoke/meta.env" <<'EOF'
ADMIN_AUTOPILOT_MODE=1
MODULES=admin,backend
EOF

OUTPUT_FILE="${TEST_ROOT}/completion.env"
python3 "${TEST_PROJECT}/scripts/overnight-completion-check.py" \
  --project-root "${TEST_PROJECT}" \
  --output "${OUTPUT_FILE}"

grep -q '^PENDING_COUNT=0$' "${OUTPUT_FILE}" || {
  KEEP_TEST_ROOT=1
  echo "expected PENDING_COUNT=0"
  cat "${OUTPUT_FILE}"
  cat "${TEST_ROOT}/completion-summary.txt"
  exit 1
}

grep -q '后台专项所有机器可判定检查项均已满足' "${TEST_ROOT}/completion-summary.txt" || {
  KEEP_TEST_ROOT=1
  echo "expected all checks passed summary"
  cat "${TEST_ROOT}/completion-summary.txt"
  exit 1
}

python3 - "${TEST_PROJECT}/output/overnight/latest-meta.env" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line != "ADMIN_GATE_EXIT_CODE=0"]
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

python3 "${TEST_PROJECT}/scripts/overnight-completion-check.py" \
  --project-root "${TEST_PROJECT}" \
  --output "${OUTPUT_FILE}" || SECOND_EXIT_CODE=$?

SECOND_EXIT_CODE="${SECOND_EXIT_CODE:-0}"
[[ "${SECOND_EXIT_CODE}" -eq 1 ]] || {
  KEEP_TEST_ROOT=1
  echo "expected second completion check to exit with code 1"
  echo "SECOND_EXIT_CODE=${SECOND_EXIT_CODE}"
  cat "${OUTPUT_FILE}"
  cat "${TEST_ROOT}/completion-summary.txt"
  exit 1
}

grep -q '^PENDING_COUNT=1$' "${OUTPUT_FILE}" || {
  KEEP_TEST_ROOT=1
  echo "expected PENDING_COUNT=1 after removing admin gate exit code"
  cat "${OUTPUT_FILE}"
  cat "${TEST_ROOT}/completion-summary.txt"
  exit 1
}

grep -q '最近一轮后台门禁验证通过' "${TEST_ROOT}/completion-summary.txt" || {
  KEEP_TEST_ROOT=1
  echo "expected missing admin gate check in summary"
  cat "${TEST_ROOT}/completion-summary.txt"
  exit 1
}

echo "completion check test passed"
