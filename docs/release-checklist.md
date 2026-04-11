# ShareHub 后台专项发布检查清单

## 发布前

- 当前分支代码已 push
- 关键文档已同步：
  - `docs/admin-automation-runbook.md`
  - `docs/admin-release-gates.md`
  - `docs/admin-rollback-checklist.md`
  - `docs/deployment-runbook.md`
  - `docs/release-checklist.md`
- 后端健康检查通过
- 前端可正常打开
- 后台专项 smoke 通过
- 当前轮次 `latest-meta.env` 已产生并包含后台专项状态码
- 后台专项 smoke 执行命令已按当前联调地址复核：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
PLAYWRIGHT_ADMIN_USER_KEY='playwright-admin' \
npx playwright test tests/e2e/admin-smoke.spec.ts
```

- 若机器上已经有可用前端 dev server，也可把 `PLAYWRIGHT_BASE_URL` 覆盖成对应地址

- 已确认后台专项联调上下文：
  - 管理员身份使用 `PLAYWRIGHT_ADMIN_USER_KEY`
  - 不再依赖 `PLAYWRIGHT_ADMIN_TOKEN`
  - 验证范围只包含 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 已确认生产链路默认禁用 `X-Admin-Token`
- 已确认生产配置满足 PostgreSQL-only
- 已确认 readiness / liveness 可访问

## 发布中

- 确认后端已按目标环境变量启动
- 确认前端代理目标正确
- 打开 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs` 检查核心入口
- 检查飞书通知链路可用

## 发布后

- 检查 `/actuator/health`
- 检查 `/actuator/health/readiness`
- 检查 `/actuator/health/liveness`
- 检查后台页面权限拦截正常
- 检查后台审计日志页可读取真实记录
- 检查最近一次 Playwright 报告已生成
- 若使用夜间脚本执行，确认报告位于 `output/overnight/*/browser-smoke/playwright-report`
- 若使用夜间脚本执行，确认 `output/overnight/latest-meta.env` 中 `ADMIN_AUTH_EXIT_CODE=0`、`ADMIN_SMOKE_EXIT_CODE=0`、`ADMIN_GATE_EXIT_CODE=0`

## 回滚条件

- 后端健康检查失败
- 后台权限失效
- 后台专项 smoke 失败
- 自动化持续异常且无法自愈

## 回滚动作

- 回退到上一稳定提交
- 重启后端与前端
- 重新执行后台专项 smoke
- 在飞书同步回滚原因和当前状态
