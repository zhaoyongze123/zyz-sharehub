# 后台专项发布门禁清单

## 必过项

- 后台专项 prompt 已切换
- 后台专项 completion check 已切换
- `ADMIN_AUTH_EXIT_CODE=0`
- `ADMIN_SMOKE_EXIT_CODE=0`
- `ADMIN_GATE_EXIT_CODE=0`
- 后台页面真实接口 smoke 通过
- 后台专项 smoke 仅执行 `frontend/tests/e2e/admin-smoke.spec.ts`
- smoke 只覆盖 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 生产配置满足 PostgreSQL-only
- readiness / liveness 已开启
- `application.yml`、`application-test.yml`、`application-cloud-dev.yml` 默认禁用 dev token，只有显式设置 `SHAREHUB_ADMIN_DEV_TOKEN_ENABLED=true` 才允许本地、测试、cloud-dev 联调
- 生产链路拒绝仅凭 `X-Admin-Token` 访问后台接口
- 小时调度日志已输出 `ADMIN_AUTH_EXIT_CODE`、`ADMIN_SMOKE_EXIT_CODE`、`ADMIN_GATE_EXIT_CODE`

## 阻断项

- 非管理员仍可访问后台
- 后台仍默认依赖 `X-Admin-Token`
- 后台 smoke 仍走 token 伪管理员路径
- 后台专项 smoke 仍复用通用 spec 集合而非独立 admin spec
- 后台 smoke 混入 `/admin/taxonomy`、公开站点或全站走查
- 生产配置仍存在 MySQL
- `latest-meta.env` 缺失任一 `ADMIN_*_EXIT_CODE`
