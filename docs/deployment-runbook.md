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
npx playwright test tests/e2e/full-site-walkthrough.spec.ts
```

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
