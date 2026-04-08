# 飞书到 Codex CLI 桥接

## 目标

收到飞书消息后，在本机执行 `codex exec`，再把执行结果回发到当前飞书会话。

## 当前实现

- 接口脚本：`scripts/feishu_codex_bridge.py`
- 回调地址：`POST /feishu/events`
- 健康检查：`GET /healthz`
- 执行方式：调用本机 `codex exec --json`
- 消息回发：复用 `scripts/feishu_notify.py`

## 启动命令

```bash
cd /Users/mac/Documents/New\ project
FEISHU_VERIFICATION_TOKEN=你的回调校验 token \
FEISHU_ALLOWED_OPEN_IDS=你的_open_id \
CODEX_BRIDGE_WORKDIR=/Users/mac/Documents/New\ project \
python3 scripts/feishu_codex_bridge.py
```

默认监听：

```text
127.0.0.1:8765
```

## 最小验证

```bash
cd /Users/mac/Documents/New\ project
python3 -m unittest tests/test_feishu_codex_bridge.py
```

## 可选环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `CODEX_BRIDGE_HOST` | 监听地址 | `127.0.0.1` |
| `CODEX_BRIDGE_PORT` | 监听端口 | `8765` |
| `CODEX_BRIDGE_WORKDIR` | `codex exec` 工作目录 | 当前目录 |
| `FEISHU_VERIFICATION_TOKEN` | 飞书事件订阅校验 token | 空 |
| `FEISHU_ALLOWED_OPEN_IDS` | 允许触发任务的发送者 open_id，多个用逗号分隔 | 空，表示不限制 |
| `FEISHU_BOT_OPEN_ID` | 机器人自身 open_id，用于忽略自发消息 | 空 |
| `CODEX_BRIDGE_PROMPT_PREFIX` | 注入到 `codex exec` 前的固定前缀 | 内置中文前缀 |
| `CODEX_BRIDGE_WORKERS` | 异步执行线程数 | `2` |

## 飞书后台配置

事件订阅至少需要：

- `im.message.receive_v1`

回调 URL 示例：

```text
https://你的域名/feishu/events
```

如果你本地调试，可先用反向代理或隧道工具把本地 `8765` 暴露出去。

## 消息处理规则

- 只处理文本消息
- 优先回到原始 `chat_id`
- 如果配置了 `FEISHU_ALLOWED_OPEN_IDS`，则仅允许白名单发送者触发
- 如果配置了 `FEISHU_BOT_OPEN_ID`，则会忽略机器人自己发出的消息

## 已知限制

- 目前未实现飞书加密回调解密
- 目前未做事件去重持久化
- 目前每条飞书消息都会启动一次独立 `codex exec`
- 目前回发内容是执行结果摘要，不会自动保留线程连续上下文

## 建议下一步

如果这条链路跑稳，可以继续补：

1. 事件去重
2. 多工作区路由
3. 任务状态中间通知
4. 升级到 `Codex App Server` 线程复用模式
