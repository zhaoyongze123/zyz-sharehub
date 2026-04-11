# 夜间自动推进说明

## 目标

让 ShareHub 项目由单一 `supervisor` 常驻推进后台管理生产级改造专项：启动立即执行一轮，之后每轮结束自动触发下一轮，到次日早上 9 点停止。

当前夜间链路只服务后台专项，唯一方向如下：

- 仅推进 `/admin` 页面、`/api/admin/**` 接口及其专属门禁、运行文档
- 只接受 GitHub OAuth + PostgreSQL 管理员白名单
- 生产链路默认禁用 `X-Admin-Token`
- 只验后台专项 smoke，不再做公开站点、资源广场、路线广场、发布页或全站走查

## 当前完成判定

后台专项完成判定由 [scripts/overnight-completion-check.py](/Users/mac/Documents/New project/scripts/overnight-completion-check.py) 执行，当前只检查后台专项机器可判定条件：

- `scripts/overnight-manager-prompt.md` 只描述后台专项
- `scripts/overnight-hourly-run.sh` 输出 `ADMIN_AUTH_EXIT_CODE`、`ADMIN_SMOKE_EXIT_CODE`、`ADMIN_GATE_EXIT_CODE`
- `scripts/overnight-browser-smoke.sh` 只执行后台专项 smoke，并检查 PostgreSQL-only 与 probes
- `frontend/tests/e2e/admin-smoke.spec.ts` 只覆盖 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 最近一轮 `latest-meta.env` 和 `browser-smoke/meta.env` 显示后台专项 smoke 与门禁通过

手动执行完成判定：

```bash
cd /Users/mac/Documents/New\ project
python3 scripts/overnight-completion-check.py \
  --project-root . \
  --output output/overnight/manual-check/meta.env
```

## 角色模型

- 经理：读取后台专项目标、拆分并审核两条工作线结果
- 线 A：后台鉴权与安全收口
- 线 B：后台功能与可用性门禁

如果 CLI 环境支持并行，则按两条工作线并行执行；如果环境不支持并行，则需要在轮次结果里明确写出退化原因。

## 启动方式

```bash
cd /Users/mac/Documents/New\ project
export POSTGRES_PASSWORD='你的开发库密码'
export REDIS_PASSWORD='你的开发 Redis 密码'
./scripts/start-overnight-autopilot.sh
```

如果不想污染当前 shell，可改为写入 `backend/.env.cloud-dev.local`，由 `scripts/run-backend-cloud-dev.sh` 和浏览器 smoke 脚本自动读取。

如果没有 cloud-dev 密码，浏览器 smoke 会自动回落到本机 smoke profile，保证后台专项链路仍可自验证。

## 停止方式

```bash
cd /Users/mac/Documents/New\ project
./scripts/stop-overnight-autopilot.sh
```

停止脚本会同时结束 `supervisor`、当前持锁轮次与保活进程，并清理 `output/overnight/state/hourly-run.lock`，避免残留锁阻塞下一次启动。

## 输出位置

- supervisor 日志：`output/overnight/supervisor.log`
- 最近一轮日志：`output/overnight/latest.log`
- 最近一轮总结：`output/overnight/latest-message.md`
- 运行状态：`output/overnight/latest-meta.env`
- 单轮执行锁：`output/overnight/state/hourly-run.lock/owner.env`
- 诊断脚本：`scripts/overnight-monitor.sh`
- 浏览器 smoke 日志：`output/overnight/<RUN_ID>/browser-smoke/smoke.log`
- 后端联调日志：`output/overnight/<RUN_ID>/browser-smoke/backend.log`
- 前端联调日志：`output/overnight/<RUN_ID>/browser-smoke/frontend.log`
- Playwright 报告：`output/overnight/<RUN_ID>/browser-smoke/playwright-report`
- 后端模式标记：`output/overnight/<RUN_ID>/browser-smoke/backend-mode.txt`
- 完成判定摘要：`output/overnight/manual-check/completion-summary.txt`

## 关键实现

- 每次启动后立即执行一轮
- `supervisor` 是唯一常驻进程，不再依赖 `launchd`
- 当前轮次结束后，`supervisor` 会自动启动下一轮
- 如果单轮执行超时、保活进程退出或状态文件异常，`supervisor` 会自动自愈
- 夜间值守使用独立 `output/overnight/codex-home` 作为 `CODEX_HOME`，避免和桌面会话共享状态库
- 到 09:00 后 supervisor 自动退出
- 每轮调用 `codex exec`
- 默认单轮超时 3000 秒，避免单次卡死整夜
- `hourly-run` 通过 `output/overnight/state/hourly-run.lock` 保证同一时刻只会有一个轮次执行；重复触发时会直接跳过
- `hourly-run` 启动时会先把自身脚本复制到 `output/overnight/state/hourly-run.exec.sh` 再执行，避免值守过程中脚本被覆盖后同一轮前后逻辑漂移
- 每轮默认按线 A 与线 B 并行执行；若退化为串行，结果中必须明确说明
- 浏览器 smoke 只执行 `frontend/tests/e2e/admin-smoke.spec.ts`
- 浏览器 smoke 会检查 `/actuator/health`、`/actuator/health/readiness`、`/actuator/health/liveness`
- 浏览器 smoke 会阻断非 PostgreSQL-only 生产配置
- 浏览器 smoke 会阻断生产环境接受独立 `X-Admin-Token`
- 后台专项模式下已禁用公开站点前端跟进子代理
- 每轮结束会自动 push 当前功能分支并发飞书总结
- 如果自动化或业务阻塞导致无法继续推进，会发飞书异常通知

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
  - 默认 `9`
- `OVERNIGHT_TIMEOUT_SECONDS`
  - 默认 `3000`
- `OVERNIGHT_ADMIN_AUTOPILOT`
  - 默认 `1`
- `OVERNIGHT_ADMIN_PARALLEL_MODE`
  - 默认 `2way`
- `OVERNIGHT_ADMIN_REQUIRE_POSTGRES`
  - 默认 `1`

例如：

```bash
OVERNIGHT_DEADLINE_HOUR=9 OVERNIGHT_TIMEOUT_SECONDS=2400 ./scripts/start-overnight-autopilot.sh
```
