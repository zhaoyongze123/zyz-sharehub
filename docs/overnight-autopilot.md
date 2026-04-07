# 夜间自动推进说明

## 目标

让 ShareHub 项目从当前时刻开始，按每小时一轮的方式持续推进到次日早上 9 点。

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

- 总日志：`output/overnight/loop.log`
- 最近一轮日志：`output/overnight/latest.log`
- 最近一轮总结：`output/overnight/latest-message.md`
- 运行状态：`output/overnight/latest-meta.env`

## 关键实现

- 每次启动后立即执行一轮
- 之后每到下一个整点再执行一轮
- 默认在次日 09:00 自动停止
- 每轮调用 `codex exec`
- 默认单轮超时 3000 秒，避免单次卡死整夜
- 使用 `caffeinate -dimsu` 保持机器不休眠

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
  - 默认 `9`
- `OVERNIGHT_TIMEOUT_SECONDS`
  - 默认 `3000`

例如：

```bash
OVERNIGHT_DEADLINE_HOUR=9 OVERNIGHT_TIMEOUT_SECONDS=2400 ./scripts/start-overnight-autopilot.sh
```
