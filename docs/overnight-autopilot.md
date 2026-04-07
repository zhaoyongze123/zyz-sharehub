# 夜间自动推进说明

## 目标

让 ShareHub 项目从当前时刻开始，按每小时一轮推进，并由 5 分钟巡检自动修复，到次日早上 9 点停止。

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
- 之后由 `supervisor` 常驻进程每 5 分钟巡检
- `supervisor` 会在整点附近自动触发每小时批次
- 如果调度、保活或最近执行状态异常，会自动补拉或补跑
- 到 09:00 后 supervisor 自动退出
- 每轮调用 `codex exec`
- 默认单轮超时 3000 秒，避免单次卡死整夜
- 使用 `caffeinate -dimsu -t` 保持机器不休眠直到截止时间
- 每轮结束会发飞书通知；如果巡检检测到异常并修复，也会发飞书通知

## 可调参数

- `OVERNIGHT_DEADLINE_HOUR`
  - 默认 `9`
- `OVERNIGHT_TIMEOUT_SECONDS`
  - 默认 `3000`

例如：

```bash
OVERNIGHT_DEADLINE_HOUR=9 OVERNIGHT_TIMEOUT_SECONDS=2400 ./scripts/start-overnight-autopilot.sh
```
