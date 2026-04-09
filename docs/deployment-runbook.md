# ShareHub 部署运行手册

## 目标

用于把当前仓库按真实链路启动起来，并在部署或联调时快速定位问题。

## 启动前检查

- 确认当前分支与提交版本
- 确认 Node.js、JDK 17、npm、Maven 已安装
- 确认 PostgreSQL / Redis 可用
- 若走 cloud-dev，确认 `backend/.env.cloud-dev.local` 中密码已配置

## 后端启动

```bash
cd /Users/mac/Documents/New\ project
export JAVA_HOME='/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
./scripts/run-backend-cloud-dev.sh
```

健康检查：

```bash
curl http://127.0.0.1:18080/actuator/health
```

## 前端启动

```bash
cd /Users/mac/Documents/New\ project/frontend
export VITE_API_PROXY_TARGET='http://127.0.0.1:18080'
npm run dev -- --host 127.0.0.1 --port 14173 --strictPort
```

访问地址：

- 前端：`http://127.0.0.1:14173`
- 后端：`http://127.0.0.1:18080`

## 浏览器联调

最小 smoke：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
npx playwright test tests/e2e/module-smoke.spec.ts
```

全站走查：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
PLAYWRIGHT_USER_KEY='playwright-user' \
PLAYWRIGHT_ADMIN_TOKEN='dev-admin-token' \
npx playwright test tests/e2e/full-site-walkthrough.spec.ts
```

说明：

- `full-site-walkthrough.spec.ts` 会通过真实接口创建 1 条笔记、1 条资料、1 条路线并追加真实节点，用于页面闭环验证
- 后台治理页依赖管理员透传头；默认联调值为 `dev-admin-token`
- 若目标环境改了测试账号或管理员 token，需同步覆盖 `PLAYWRIGHT_USER_KEY`、`PLAYWRIGHT_ADMIN_TOKEN`
- 若本机已存在前端开发服务，例如 `http://127.0.0.1:5173`，可直接覆盖 `PLAYWRIGHT_BASE_URL` 复用该服务执行走查

最近一次本地复核：

- 复核时间：2026-04-10 07:05 +0800
- 复核环境：前端 `http://127.0.0.1:14173`，后端 `http://127.0.0.1:18080`
- 复核命令：`npx playwright test tests/e2e/full-site-walkthrough.spec.ts`
- 复核结果：`tests/e2e/full-site-walkthrough.spec.ts` 7/7 通过（13.2s，本轮夜间复核）

## 夜间自动化

启动：

```bash
cd /Users/mac/Documents/New\ project
./scripts/start-overnight-autopilot.sh
```

状态查看：

```bash
cd /Users/mac/Documents/New\ project
bash scripts/overnight-monitor.sh
tail -f output/overnight/supervisor.log
```

停止：

```bash
cd /Users/mac/Documents/New\ project
./scripts/stop-overnight-autopilot.sh
```

## 常见故障

### 后端启动失败

- 检查 `backend/.env.cloud-dev.local`
- 检查 PostgreSQL / Redis 端口是否被占用
- 检查 `output/overnight/*/browser-smoke/backend.log`

### 前端启动失败

- 检查 `14173` 端口是否被占用
- 检查 `VITE_API_PROXY_TARGET` 是否指向正确后端
- 检查 `output/overnight/*/browser-smoke/frontend.log`

### Playwright 失败

- 先看 `output/overnight/*/browser-smoke/smoke.log`
- 再看 `playwright-report`
- 区分是页面断言失败、接口 4xx/5xx，还是服务未启动
