# 后台专项故障回滚检查项

## 回滚触发

- 后台专项 smoke 连续失败
- 后台鉴权失效
- 后台门禁检查失败
- 夜间自动化无法继续推进并持续阻塞

## 回滚动作

```bash
cd /Users/mac/Documents/New\ project
git log --oneline -n 5
git checkout <上一稳定提交>
```

然后重新验证：

- `output/overnight/supervisor.log`
- `output/overnight/latest-meta.env`
- `ADMIN_AUTH_EXIT_CODE` / `ADMIN_SMOKE_EXIT_CODE` / `ADMIN_GATE_EXIT_CODE`
- `backend / frontend` 启动状态
- 后台接口访问控制
- `application.yml` / `application-test.yml` / `application-cloud-dev.yml` 是否恢复为默认禁用 dev token，且仅允许通过 `SHAREHUB_ADMIN_DEV_TOKEN_ENABLED=true` 显式开启
- smoke 是否仍只覆盖 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
