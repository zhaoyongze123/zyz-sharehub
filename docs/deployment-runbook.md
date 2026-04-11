# ShareHub 后台专项部署运行手册

## 目标

用于按后台管理生产级改造专项启动真实链路，并在本地、staging、production 三套环境下快速定位问题。

当前部署基线固定为：

- 生产与 staging 都是 PostgreSQL-only
- 管理员登录只接受 GitHub OAuth + PostgreSQL 白名单
- 前端构建产物由 Nginx 直接托管 `frontend/dist`
- `X-Admin-Token` 不能作为生产后台鉴权方式

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

## 后台专项浏览器联调

后台专项 smoke：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
PLAYWRIGHT_ADMIN_USER_KEY='playwright-admin' \
npx playwright test tests/e2e/admin-smoke.spec.ts
```

说明：

- `admin-smoke.spec.ts` 会通过真实后台接口校验 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 管理员联调身份使用 `PLAYWRIGHT_ADMIN_USER_KEY`，不再依赖 `PLAYWRIGHT_ADMIN_TOKEN`
- 若目标环境改了联调管理员账号，需同步覆盖 `PLAYWRIGHT_ADMIN_USER_KEY`
- 生产链路必须拒绝仅携带 `X-Admin-Token` 的后台请求
- 生产配置必须满足 PostgreSQL-only
- 发布前需确认 `/actuator/health`、`/actuator/health/readiness`、`/actuator/health/liveness` 全部可访问
- 若本机已存在前端开发服务，例如 `http://127.0.0.1:5173`，可直接覆盖 `PLAYWRIGHT_BASE_URL` 复用该服务执行走查

最近一次本地复核：

- 复核时间：2026-04-12 02:26:46 +0800
- 复核环境：前端 `http://127.0.0.1:14173`，后端 `http://127.0.0.1:18080`
- 复核命令：`npx playwright test tests/e2e/admin-smoke.spec.ts`
- 复核结果：以当前后台专项 smoke 与夜间门禁结果为准

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

## 服务器部署

### 环境文件

服务器部署目录固定为 `/opt/zyz-sharehub/deploy`，至少需要：

- `.env.prod`
- `.env.staging`

生产与 staging 都必须填写：

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

对象存储 `DO_SPACES_*` 当前不是启动必填项。

### 启动命令

```bash
cd /opt/zyz-sharehub/deploy
docker network create sharehub-public || true
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

### 现场验收

```bash
cd /opt/zyz-sharehub/deploy
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.staging -f docker-compose.staging.yml ps
curl -fsS http://127.0.0.1:8080/actuator/health
curl -k https://your-domain.com/actuator/health
curl -k https://staging.your-domain.com/actuator/health
curl -k https://your-domain.com/api/auth/github/login
curl -k https://staging.your-domain.com/api/auth/github/login
```

验收时必须同时确认：

- `sharehub-backend` 与 `sharehub-staging-backend` 都已启动
- PostgreSQL / Redis 容器健康检查通过
- 正式域名与 `staging` 域名都能返回 GitHub 登录入口
- 返回的 `loginUrl` 为 `/oauth2/authorization/github`

### 前端发布

```bash
cd /opt/zyz-sharehub/frontend
npm install
npm run build
```

构建结果固定同步到：

- `/opt/zyz-sharehub/frontend/dist`

Nginx 会直接托管该目录，不再额外启动 frontend 容器。

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
- 区分是后台页面断言失败、后台接口 4xx/5xx、PostgreSQL-only 门禁失败，还是服务未启动

### 服务器 GitHub 登录失败

- 检查 `.env.prod` / `.env.staging` 里的 `GITHUB_CLIENT_ID` 与 `GITHUB_CLIENT_SECRET` 是否对应正确环境
- 检查 GitHub OAuth App 的 Homepage / Callback 是否与访问域名一致
- 检查 Nginx 是否已代理 `/oauth2/` 与 `/login/oauth2/`
- 检查浏览器实际跳转是否进入 GitHub 授权页
