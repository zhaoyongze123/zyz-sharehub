#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

assert_contains() {
  local file="$1"
  local pattern="$2"
  if ! rg -q "${pattern}" "${file}"; then
    echo "expected pattern not found: ${pattern}" >&2
    exit 1
  fi
}

assert_not_contains() {
  local file="$1"
  local pattern="$2"
  if rg -q "${pattern}" "${file}"; then
    echo "unexpected pattern found: ${pattern}" >&2
    exit 1
  fi
}

SMOKE_SCRIPT="${PROJECT_ROOT}/scripts/overnight-browser-smoke.sh"

assert_contains "${SMOKE_SCRIPT}" 'ADMIN_SMOKE_SPECS=\('
assert_contains "${SMOKE_SCRIPT}" 'tests/e2e/admin-smoke.spec.ts'
assert_contains "${SMOKE_SCRIPT}" 'PLAYWRIGHT_MODULES:-admin,backend'
assert_contains "${SMOKE_SCRIPT}" 'validate_admin_modules'
assert_contains "${SMOKE_SCRIPT}" '后台专项 smoke 仅允许 admin/backend 模块，检测到非法模块'
assert_contains "${SMOKE_SCRIPT}" 'validate_admin_scope'
assert_contains "${SMOKE_SCRIPT}" '后台 smoke 缺少后台页面验收路由'
assert_not_contains "${SMOKE_SCRIPT}" '\$\{PROJECT_ROOT\}/scripts/overnight-browser-smoke\.sh'
assert_contains "${SMOKE_SCRIPT}" '/actuator/health/readiness'
assert_contains "${SMOKE_SCRIPT}" '/actuator/health/liveness'
assert_contains "${SMOKE_SCRIPT}" '未返回 status=UP'
assert_contains "${SMOKE_SCRIPT}" 'check_health_status_up'
assert_contains "${SMOKE_SCRIPT}" 'probe_status_summary'
assert_contains "${SMOKE_SCRIPT}" 'HTTP %s body=%s'
assert_contains "${SMOKE_SCRIPT}" '/actuator/health"'
assert_contains "${SMOKE_SCRIPT}" '/actuator/health/readiness"'
assert_contains "${SMOKE_SCRIPT}" '/actuator/health/liveness"'
assert_contains "${SMOKE_SCRIPT}" '生产部署仍然包含 MySQL 配置，未满足 PostgreSQL-only'
assert_contains "${SMOKE_SCRIPT}" '生产部署未显式使用 PostgreSQL JDBC，未满足 PostgreSQL-only'
assert_contains "${SMOKE_SCRIPT}" '生产环境错误接受了 X-Admin-Token'
assert_not_contains "${SMOKE_SCRIPT}" 'module-smoke\.spec\.ts'
assert_not_contains "${SMOKE_SCRIPT}" 'sharehub-real-api\.spec\.ts'
assert_not_contains "${SMOKE_SCRIPT}" 'full-site-walkthrough\.spec\.ts'
assert_not_contains "${SMOKE_SCRIPT}" 'STANDARD_SMOKE_SPECS'

echo "overnight browser smoke admin gates test passed"
