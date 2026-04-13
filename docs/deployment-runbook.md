# ShareHub 后台专项部署运行手册

## 目标

用于按后台管理生产级改造专项启动真实链路，并在本地、staging、production 三套环境下快速定位问题。

当前部署基线固定为：

- 生产与 staging 都是 PostgreSQL-only
- 管理员登录只接受 GitHub OAuth + PostgreSQL 白名单
- 前端构建产物由 Nginx 直接托管 `frontend/dist`
- 宿主机 `80/443` 只由系统级 Nginx / Caddy 占用
- `prod nginx` 与 `staging nginx` 分别绑定 `127.0.0.1:19080` / `127.0.0.1:19081`
- `X-Admin-Token` 不能作为生产后台鉴权方式

## 本地联调

### 后端启动

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

### 前端启动

```bash
cd /Users/mac/Documents/New\ project/frontend
export VITE_API_PROXY_TARGET='http://127.0.0.1:18080'
npm run dev -- --host 127.0.0.1 --port 14173 --strictPort
```

### 后台专项 smoke

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
PLAYWRIGHT_ADMIN_USER_KEY='playwright-admin' \
npx playwright test tests/e2e/admin-smoke.spec.ts
```

说明：

- `admin-smoke.spec.ts` 通过真实后台接口校验 `/admin`、`/admin/reports`、`/admin/reviews`、`/admin/users`、`/admin/audit-logs`
- 管理员联调身份使用 `PLAYWRIGHT_ADMIN_USER_KEY`
- 生产链路必须拒绝仅携带 `X-Admin-Token` 的后台请求

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
- `GITHUB_REDIRECT_URI`

对象存储 `DO_SPACES_*` 当前不是启动必填项。

### 启动命令

必须显式指定不同 compose project：

```bash
cd /opt/zyz-sharehub/deploy
docker compose -p sharehub-prod --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose -p sharehub-staging --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

### 系统级入口

宿主机只保留一个系统级 Nginx / Caddy：

- `zyzsharehub.cn` / `www.zyzsharehub.cn` -> `127.0.0.1:19080`
- `staging.zyzsharehub.cn` -> `127.0.0.1:19081`

容器内 Nginx 不再直接绑定宿主机 `80/443`。
系统级证书路径默认走项目内 `deploy/certbot/conf/live/...`。

### 现场验收

```bash
cd /opt/zyz-sharehub/deploy
docker compose -p sharehub-prod --env-file .env.prod -f docker-compose.prod.yml ps
docker compose -p sharehub-staging --env-file .env.staging -f docker-compose.staging.yml ps
ss -lntp | egrep ':80 |:443 |:19080 |:19081 '
curl -fsS http://127.0.0.1:19080/actuator/health
curl -fsS http://127.0.0.1:19081/actuator/health
curl -k https://your-domain.com/actuator/health
curl -k https://staging.your-domain.com/actuator/health
curl -k https://your-domain.com/api/auth/github/login
curl -k https://staging.your-domain.com/api/auth/github/login
```

验收时必须同时确认：

- `sharehub-backend` 与 `sharehub-staging-backend` 都已启动
- PostgreSQL / Redis 容器健康检查通过
- 系统级 Nginx 已监听 `80/443`
- 两个内部入口已监听 `127.0.0.1:19080` / `127.0.0.1:19081`
- 正式域名与 `staging` 域名都能返回 GitHub 登录入口
- 返回的 `loginUrl` 为 `/oauth2/authorization/github`

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

- 检查 `.env.prod` / `.env.staging` 是否把 prod / staging 指到同一个数据库
- 检查 PostgreSQL / Redis 健康检查是否通过
- 检查 backend 容器日志是否出现非 PostgreSQL JDBC

### 公网域名无法访问

- 检查系统级 Nginx / Caddy 是否监听 `80/443`
- 检查 `127.0.0.1:19080` / `127.0.0.1:19081` 是否被容器内 Nginx 占用
- 检查系统级入口是否把域名转发到了正确内部端口

### GitHub 登录失败

- 检查 `.env.prod` / `.env.staging` 里的 `GITHUB_CLIENT_ID` 与 `GITHUB_CLIENT_SECRET` 是否对应正确环境
- 检查 GitHub OAuth App 的 Homepage / Callback 是否与访问域名一致
- 检查容器内 Nginx 是否已代理 `/oauth2/` 与 `/login/oauth2/`
- 检查浏览器实际跳转是否进入 GitHub 授权页
