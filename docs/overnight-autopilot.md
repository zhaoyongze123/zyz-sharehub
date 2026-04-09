# 夜间自动推进说明

## 目标

让 ShareHub 项目从当前时刻开始，由单一 `supervisor` 常驻推进：启动立即执行一轮，之后每轮结束自动触发下一轮，到次日早上 9 点停止。

说明：
- “整站完成”按当前产品边界判定，不等于所有页面都必须接后端真接口。
- 简历模块当前定义为纯前端界面能力，因此夜间自动化对简历只要求前端 smoke / 交互验收通过，不再要求 `/api/resumes/*` 真接口联调。
- 进一步地，当前整站完成判定已不再把简历模块列为硬门槛，避免自动化继续围绕简历后端链路空转。

## 当前完成态

- 批次 1-3 已完成，当前整站完成判定依赖批次 4 的全站浏览器走查与收尾材料一致性
- 批次 4 的核心验证命令如下：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
npx playwright test tests/e2e/full-site-walkthrough.spec.ts
```

- 若前端已通过 `npm run dev` 运行在 `5173`，可直接把 `PLAYWRIGHT_BASE_URL` 改为 `http://127.0.0.1:5173` 复用现有服务，无需额外起一份 preview。
- 最近一次人工复核：2026-04-10 03:43 +0800，使用前端 `http://127.0.0.1:14173`、后端 `http://127.0.0.1:18080` 执行 `full-site-walkthrough.spec.ts`，结果 7/7 通过（13.4s）。

- 只有当上述走查通过，且以下材料都存在并与当前联调方式一致时，夜间自动化才应判定整站完成：
  - `docs/deployment-runbook.md`
  - `docs/release-checklist.md`
  - `docs/demo-script.md`

## 角色模型

- 经理：读取需求、拆分任务、派发、审核、收口
- 开发：实现代码、修复问题
- 测试：做真实验证、回归和记录问题

如果 CLI 环境支持子 agent，则按三角色并行执行；如果不支持，则在一轮内按三角色顺序执行。

## 启动方式

```bash
cd /Users/mac/Documents/New\ project
export POSTGRES_PASSWORD='你的开发库密码'
export REDIS_PASSWORD='你的开发 Redis 密码'
./scripts/start-overnight-autopilot.sh
```

如果不想污染当前 shell，可改为写入 `backend/.env.cloud-dev.local`，由 `scripts/run-backend-cloud-dev.sh` 和浏览器 smoke 脚本自动读取。

如果没有 cloud-dev 密码，浏览器联调会自动回落到本机自包含 smoke profile（`test` profile + H2），保证夜间链路仍可自验证。

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
- 前端子代理日志：`output/overnight/<RUN_ID>/frontend-followup/codex-output.log`
- 前端子代理 prompt：`output/overnight/<RUN_ID>/frontend-followup/prompt.txt`

## 关键实现

- 每次启动后立即执行一轮
- `supervisor` 是唯一常驻进程，不再依赖 `launchd`
- 当前轮次结束后，`supervisor` 会自动启动下一轮
- 如果单轮执行超时、保活进程退出或状态文件异常，`supervisor` 会自动自愈
- `supervisor` 会在每轮结束后执行一次“整站完成判定”；若所有机器可判定验收项都满足，则自动停止，不再继续下一轮
- 整站完成判定以当前产品边界为准；已被明确降级为纯前端或非夜间目标的模块，不应继续作为阻塞项
- 当判定整站已完成时，会自动发一条飞书通知：`ShareHub 夜间推进已全部完成`
- 夜间值守使用独立 `output/overnight/codex-home` 作为 `CODEX_HOME`，避免和桌面会话共享状态库
- 到 09:00 后 supervisor 自动退出
- 每轮调用 `codex exec`
- 默认单轮超时 3000 秒，避免单次卡死整夜
- `hourly-run` 通过 `output/overnight/state/hourly-run.lock` 保证同一时刻只会有一个轮次执行；重复触发时会直接跳过，不再并发拉起第二轮
- `hourly-run` 启动时会先把自身脚本复制到 `output/overnight/state/hourly-run.exec.sh` 再执行，避免值守过程中脚本被覆盖后同一轮前后逻辑漂移
- 每轮代码成功后会自动启动前后端，并执行对应模块的 Playwright 浏览器 smoke；联调失败则本轮失败，不按成功态继续推进
- 完成判定会区分模块边界：资源、路线、个人中心、后台治理等继续按真接口联调验收；简历模块按纯前端页面验收
- 当批次 1-3 收口后，夜间自动化会继续进入“批次 4：全站浏览器走查 + 收尾材料”
- 批次 4 会执行全站 Playwright 走查；若失败，则进入“修复 -> 回归 -> 再走查”的闭环
- 当全站走查通过且部署 / 发布 / 演示材料齐备时，才判定整站真正完成
- 当浏览器 smoke 成功后，会继续派发一个独立前端跟进子代理，只处理本轮已通过联调的模块对应前端页面，并推送到独立前端分支
- 浏览器联调优先走 cloud-dev 后端；若缺少 PostgreSQL/Redis 密码，则自动回落到本机 smoke profile，避免整夜因环境变量缺失直接停摆
- 浏览器 smoke 在健康检查成功后，还会对前后端分别做一次短时稳定性校验，避免服务刚启动即退出却被误判为可用
- 使用 `caffeinate -dimsu -t` 保持机器不休眠直到截止时间
- 每轮结束会自动 push 当前分支并发飞书总结
- 如果自动化或业务阻塞导致无法继续推进，会发飞书异常通知

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
  - 默认 `9`
- `OVERNIGHT_TIMEOUT_SECONDS`
  - 默认 `3000`
- `OVERNIGHT_FRONTEND_FOLLOWUP_MODEL`
  - 默认 `gpt-5.1-codex-max`
- `OVERNIGHT_FRONTEND_FOLLOWUP_TIMEOUT_SECONDS`
  - 默认 `2400`
- `OVERNIGHT_FRONTEND_FOLLOWUP_ALL_MODULES`
  - 默认 `0`，表示前端子代理只跟进一个最适合闭环的模块；设为 `1` 时才会尝试同轮覆盖多个模块
- `OVERNIGHT_FRONTEND_FOLLOWUP_DRY_RUN`
  - 设为 `1` 时只生成前端子代理 prompt 和分支计划，不实际执行

例如：

```bash
OVERNIGHT_DEADLINE_HOUR=9 OVERNIGHT_TIMEOUT_SECONDS=2400 ./scripts/start-overnight-autopilot.sh
```
