# DigitalOcean 单机部署指南（PostgreSQL-only）

## 目标

当前部署方案固定为：

- 单机 DigitalOcean
- 生产与测试都只使用 PostgreSQL + Redis
- 前端不再在服务器内单独起容器，而是本地构建 `frontend/dist` 后由生产 Nginx 直接托管
- `staging.your-domain.com` 与正式域名共用同一台机器
- GitHub OAuth 按环境拆分为两个 OAuth App

## 1. 服务器初始化

```bash
ssh root@<your_server_ip>
apt update && apt upgrade -y
apt install -y ca-certificates curl git unzip

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
```

## 2. 拉取项目

```bash
cd /opt
git clone https://github.com/zhaoyongze123/zyz-sharehub.git
cd /opt/zyz-sharehub
```

## 3. 准备前端静态构建产物

当前服务器部署不再依赖前端容器 build。
推荐在本地工作机执行：

```bash
cd /Users/mac/Documents/New\ project/frontend
npm install
npm run build
```

把构建结果同步到服务器：

```bash
rsync -avz ./dist/ root@<your_server_ip>:/opt/zyz-sharehub/frontend/dist/
```

验收点：

- 服务器存在 `/opt/zyz-sharehub/frontend/dist/index.html`
- Nginx 会把该目录挂载到 `/usr/share/nginx/html`

## 4. 配置生产与测试环境变量

```bash
cd /opt/zyz-sharehub/deploy
cp .env.prod.example .env.prod
cp .env.staging.example .env.staging
vim .env.prod
vim .env.staging
```

### 生产 `.env.prod` 必填

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

### 测试 `.env.staging` 必填

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

### OAuth 回调要求

- 生产 OAuth App
  - Homepage URL: `https://your-domain.com`
  - Authorization callback URL: `https://your-domain.com/login/oauth2/code/github`
- 测试 OAuth App
  - Homepage URL: `https://staging.your-domain.com`
  - Authorization callback URL: `https://staging.your-domain.com/login/oauth2/code/github`

### 对象存储说明

当前代码主链路不是对象存储直连，因此 `DO_SPACES_*` 不是启动必填项。
只有未来把文件上传主链路切到对象存储后，才需要补齐：

- `DO_SPACES_BUCKET`
- `DO_SPACES_ACCESS_KEY`
- `DO_SPACES_SECRET_KEY`

## 5. 配置 DNS

在域名服务商新增：

- `A your-domain.com -> <your_server_ip>`
- `A www.your-domain.com -> <your_server_ip>`
- `A staging.your-domain.com -> <your_server_ip>`

## 6. 申请证书

```bash
cd /opt/zyz-sharehub/deploy
mkdir -p certbot/www certbot/conf

docker compose --env-file .env.prod -f docker-compose.prod.yml run --rm --profile certbot certbot \
  certonly --webroot -w /var/www/certbot \
  -d your-domain.com -d www.your-domain.com -d staging.your-domain.com \
  --email your@email.com --agree-tos --no-eff-email
```

## 7. 配置 Nginx

```bash
cd /opt/zyz-sharehub/deploy
cp nginx/conf.d/sharehub-ssl.conf.example nginx/conf.d/sharehub-ssl.conf
vim nginx/conf.d/sharehub-ssl.conf
```

需要确认：

- 正式域名 `/api`、`/oauth2`、`/login/oauth2`、`/actuator` 代理到 `sharehub-backend:8080`
- `staging` 域名对应路径代理到 `sharehub-staging-backend:8080`
- 两个域名的 `/` 都指向 `/usr/share/nginx/html`

## 8. 首次启动

```bash
cd /opt/zyz-sharehub/deploy

docker network create sharehub-public || true

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

预期容器：

- 生产：`sharehub-postgres` `sharehub-redis` `sharehub-backend` `sharehub-nginx`
- 测试：`sharehub-staging-postgres` `sharehub-staging-redis` `sharehub-staging-backend`

## 9. 验收命令

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

验收标准：

- PostgreSQL / Redis 为 `healthy`
- backend / nginx 为 `Up`
- `/actuator/health` 返回 `{"status":"UP"}`
- `/api/auth/github/login` 返回 JSON，且 `loginUrl` 指向 `/oauth2/authorization/github`

## 10. 更新发布

```bash
cd /opt/zyz-sharehub
git pull

cd /opt/zyz-sharehub/frontend
npm install
npm run build

cd /opt/zyz-sharehub/deploy
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

## 11. 回滚检查项

- 先看 `docker compose ps` 是否是某个单独容器异常
- 再看 `docker compose logs backend`、`docker compose logs nginx`
- 检查 `.env.prod` / `.env.staging` 是否误写 MySQL 或空密码
- 检查服务器上的 `frontend/dist` 是否被错误删除
- 检查 OAuth App 回调地址是否与当前域名一致

## 12. 证书续期

```bash
cd /opt/zyz-sharehub/deploy
docker compose --env-file .env.prod -f docker-compose.prod.yml run --rm --profile certbot certbot renew
docker compose --env-file .env.prod -f docker-compose.prod.yml restart nginx
```
