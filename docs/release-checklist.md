# ShareHub 发布检查清单（后台专项）

## 发布前

- 当前分支代码已 push
- 关键文档已同步：
  - `docs/deployment-runbook.md`
  - `docs/deploy-digitalocean.md`
  - `docs/release-checklist.md`
- 生产配置满足 PostgreSQL-only
- `prod` / `staging` 使用不同 compose project：
  - `sharehub-prod`
  - `sharehub-staging`
- 两套数据库、Redis、OAuth App 完全隔离
- 系统级 Nginx / Caddy 已配置：
  - 正式域名 -> `127.0.0.1:19080`
  - staging 域名 -> `127.0.0.1:19081`

## 发布中

- 确认生产后端已按目标环境变量启动
- 确认 staging 后端已按目标环境变量启动
- 确认系统级入口监听 `80/443`
- 确认内部入口监听：
  - `127.0.0.1:19080`
  - `127.0.0.1:19081`
- 确认后台页面和后台接口 smoke 通过

## 发布后

- 检查 `https://zyzsharehub.cn/actuator/health`
- 检查 `https://staging.zyzsharehub.cn/actuator/health`
- 检查 `https://zyzsharehub.cn/api/auth/github/login`
- 检查 `https://staging.zyzsharehub.cn/api/auth/github/login`
- 检查后台页面权限拦截正常
- 检查后台审计日志页可读取真实记录

## 回滚条件

- 系统级 Nginx / Caddy 未监听 `80/443`
- `prod` 或 `staging` 任一内部入口不可访问
- 后端健康检查失败
- GitHub 登录入口异常
- 后台 smoke 失败

## 回滚动作

- 回退到上一稳定提交
- 重启对应环境容器
- 重新执行健康检查与后台 smoke
- 在飞书同步回滚原因和当前状态
