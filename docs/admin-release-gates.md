# 后台专项发布门禁清单

## 必过项

- 后台专项 prompt 已切换
- 后台专项 completion check 已切换
- `ADMIN_AUTH_EXIT_CODE=0`
- `ADMIN_SMOKE_EXIT_CODE=0`
- `ADMIN_GATE_EXIT_CODE=0`
- 后台页面真实接口 smoke 通过
- smoke 只覆盖 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 生产配置满足 PostgreSQL-only
- readiness / liveness 已开启

## 阻断项

- 非管理员仍可访问后台
- 后台仍默认依赖 `X-Admin-Token`
- 后台 smoke 仍走 token 伪管理员路径
- 生产配置仍存在 MySQL
- `latest-meta.env` 缺失任一 `ADMIN_*_EXIT_CODE`
