# 夜间自动推进说明

## 目标

让 ShareHub 项目从当前时刻开始，由单一 `supervisor` 常驻推进：启动立即执行一轮，之后每轮结束自动触发下一轮，到次日早上 9 点停止。

## 角色模型

- 经理：读取需求、拆分任务、派发、审核、收口
- 开发：实现代码、修复问题
- 测试：做真实验证、回归和记录问题

如果 CLI 环境支持子 agent，则按三角色并行执行；如果不支持，则在一轮内按三角色顺序执行。

## 启动方式

```bash
cd /Users/mac/Documents/New\ project
./scripts/start-overnight-autopilot.sh
```

## 停止方式

```bash
cd /Users/mac/Documents/New\ project
./scripts/stop-overnight-autopilot.sh
```

## 输出位置

- supervisor 日志：`output/overnight/supervisor.log`
- 最近一轮日志：`output/overnight/latest.log`
- 最近一轮总结：`output/overnight/latest-message.md`
- 运行状态：`output/overnight/latest-meta.env`
- 诊断脚本：`scripts/overnight-monitor.sh`

## 关键实现

- 每次启动后立即执行一轮
- `supervisor` 是唯一常驻进程，不再依赖 `launchd`
- 当前轮次结束后，`supervisor` 会自动启动下一轮
- 如果单轮执行超时、保活进程退出或状态文件异常，`supervisor` 会自动自愈
- 夜间值守使用独立 `output/overnight/codex-home` 作为 `CODEX_HOME`，避免和桌面会话共享状态库
- 到 09:00 后 supervisor 自动退出
- 每轮调用 `codex exec`
- 默认单轮超时 3000 秒，避免单次卡死整夜
- 使用 `caffeinate -dimsu -t` 保持机器不休眠直到截止时间
- 每轮结束会自动 push 当前分支并发飞书总结
- 如果自动化或业务阻塞导致无法继续推进，会发飞书异常通知

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
  - 默认 `9`
- `OVERNIGHT_TIMEOUT_SECONDS`
  - 默认 `3000`

例如：

```bash
OVERNIGHT_DEADLINE_HOUR=9 OVERNIGHT_TIMEOUT_SECONDS=2400 ./scripts/start-overnight-autopilot.sh
```
