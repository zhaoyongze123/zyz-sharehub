# 夜间自动推进说明（后台专项）

## 目标

让 ShareHub 后台管理专项从当前时刻开始，由单一 `supervisor` 常驻推进：启动立即执行一轮，之后每轮结束自动触发下一轮，到次日早上 9 点停止。

当前夜间自动化只围绕后台管理专项，不再把公开站点、简历、资源发布页等整站目标作为完成条件。

## 当前完成态定义

后台专项只有在以下条件都满足时，夜间自动化才允许判定完成：

- 后台鉴权以 GitHub OAuth + PostgreSQL 白名单为准
- `GET /api/auth/me` 可返回 `isAdmin`
- 后台写操作要求 `reason`
- 审计日志包含 `request_id` 与结果态
- 生产配置满足 PostgreSQL-only
- 后台 smoke 通过：
  - `/admin`
  - `/admin/reports`
  - `/admin/reviews`
  - `/admin/users`
  - `/admin/audit-logs`
- 发布与部署文档与当前链路一致：
  - `docs/deployment-runbook.md`
  - `docs/release-checklist.md`
  - `docs/deploy-digitalocean.md`

## 启动方式

```bash
cd /Users/mac/Documents/New\ project
export POSTGRES_PASSWORD='你的开发库密码'
export REDIS_PASSWORD='你的开发 Redis 密码'
export OVERNIGHT_ADMIN_AUTOPILOT=1
export OVERNIGHT_ADMIN_PARALLEL_MODE=2way
export OVERNIGHT_ADMIN_REQUIRE_POSTGRES=1
./scripts/start-overnight-autopilot.sh
```

## 输出位置

- supervisor 日志：`output/overnight/supervisor.log`
- 最近一轮日志：`output/overnight/latest.log`
- 最近一轮总结：`output/overnight/latest-message.md`
- 浏览器 smoke 日志：`output/overnight/<RUN_ID>/browser-smoke/smoke.log`
- 后端联调日志：`output/overnight/<RUN_ID>/browser-smoke/backend.log`
- 前端联调日志：`output/overnight/<RUN_ID>/browser-smoke/frontend.log`

## 关键实现

- 固定两条并行工作线：
  - 鉴权与安全线
  - 功能与可用性线
- 每轮完成后执行后台专项完成判定
- 失败时阻断完成态，并通过飞书发送异常通知
- 完成态输出固定包含：
  - `ADMIN_AUTH_EXIT_CODE`
  - `ADMIN_SMOKE_EXIT_CODE`
  - `ADMIN_GATE_EXIT_CODE`

## 部署前提

服务器部署必须满足：

- `prod` / `staging` 使用不同 compose project
- 两套 PostgreSQL / Redis 完全隔离
- 宿主机 `80/443` 由系统级 Nginx / Caddy 占用
- 内部入口固定为：
  - `127.0.0.1:19080` -> prod nginx
  - `127.0.0.1:19081` -> staging nginx

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
- `OVERNIGHT_TIMEOUT_SECONDS`
- `OVERNIGHT_ADMIN_AUTOPILOT`
- `OVERNIGHT_ADMIN_PARALLEL_MODE`
- `OVERNIGHT_ADMIN_REQUIRE_POSTGRES`
