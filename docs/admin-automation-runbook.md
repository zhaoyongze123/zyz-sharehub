# 后台专项运行手册

## 目标

用于 ShareHub 后台管理生产级改造专项的夜间自动化执行、联调与复核。

## 当前规则

- 仅覆盖后台管理相关前后端与 `/admin`、`/api/admin/**`
- 管理员身份以 GitHub OAuth + PostgreSQL 白名单为准
- 生产默认禁用 `X-Admin-Token`
- 夜间自动化固定按后台专项模式执行
- 小时调度已禁用公开站点前端跟进子代理

## 最小复核

```bash
cd /Users/mac/Documents/New\ project
./scripts/start-overnight-autopilot.sh
```

重点查看：

- `output/overnight/supervisor.log`
- `output/overnight/latest-meta.env`
- `output/overnight/latest-message.md`

其中 `latest-meta.env` 至少要包含并可读出：

- `ADMIN_AUTH_EXIT_CODE`
- `ADMIN_SMOKE_EXIT_CODE`
- `ADMIN_GATE_EXIT_CODE`

## 后台专项 smoke

后台 smoke 只允许覆盖：

- `/admin`
- `/admin/reports`
- `/admin/reviews`
- `/admin/users`
- `/admin/audit-logs`

执行时必须满足：

- 后台专项 smoke 入口固定为 `frontend/tests/e2e/admin-smoke.spec.ts`
- Playwright 使用 `PLAYWRIGHT_ADMIN_USER_KEY`，不再依赖 `PLAYWRIGHT_ADMIN_TOKEN`
- 仅验 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 脚本检查 `/actuator/health`、`/actuator/health/readiness`、`/actuator/health/liveness`
- 若 readiness / liveness 不可用，日志会附带探针 HTTP 状态码与响应体片段，便于区分 403、404 和启动未完成
- 脚本检查生产配置是否仍包含 MySQL，并以 PostgreSQL-only 为门禁
- 脚本检查 `application.yml`、`application-test.yml`、`application-cloud-dev.yml` 都默认关闭 dev token，只有显式设置 `SHAREHUB_ADMIN_DEV_TOKEN_ENABLED=true` 才允许本地、测试、cloud-dev 联调
- 脚本检查生产链路拒绝单独携带 `X-Admin-Token` 的后台请求
- 脚本检查 `AdminTokenFilterProdModeIntegrationTest` 与 `AdminTokenFilterDevModeIntegrationTest` 同时存在，分别覆盖“生产禁 token”和“显式开启 dev token”
- 小时调度日志必须显式输出 `ADMIN_AUTH_EXIT_CODE`、`ADMIN_SMOKE_EXIT_CODE`、`ADMIN_GATE_EXIT_CODE`
- 小时调度日志必须出现“已禁用公开站点前端跟进子代理”，确保本轮没有切回公开站点链路
