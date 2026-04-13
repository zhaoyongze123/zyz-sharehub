# DigitalOcean 单机部署指南（PostgreSQL-only + 统一入口）

## 目标

当前部署方案固定为：

- 单机 DigitalOcean
- `prod` 与 `staging` 完全隔离
- `prod` / `staging` 都只使用 PostgreSQL + Redis
- 宿主机 `80/443` 只由系统级 Nginx / Caddy 占用
- 容器内 `prod nginx` / `staging nginx` 仅绑定 `127.0.0.1` 高位端口
- GitHub OAuth 按环境拆分为两个 OAuth App

部署结构：

```text
公网 80/443
    ↓
系统级 Nginx / Caddy
    ↓
127.0.0.1:19080 -> sharehub-nginx -> sharehub-backend -> sharehub-postgres/sharehub-redis
127.0.0.1:19081 -> sharehub-staging-nginx -> sharehub-staging-backend -> sharehub-staging-postgres/sharehub-staging-redis
```

## 1. 服务器初始化

```bash
ssh root@<your_server_ip>
apt update && apt upgrade -y
apt install -y ca-certificates curl git unzip nginx

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl enable nginx
```

## 2. 拉取项目

```bash
cd /opt
git clone https://github.com/zhaoyongze123/zyz-sharehub.git
cd /opt/zyz-sharehub
```

## 3. 准备前端静态构建产物

当前服务器部署不再依赖前端容器 build。推荐在本地工作机执行：

```bash
cd /Users/mac/Documents/New\ project/frontend
npm install
npm run build
```

同步到服务器：

```bash
rsync -avz ./dist/ root@<your_server_ip>:/opt/zyz-sharehub/frontend/dist/
```

## 4. 配置环境变量

```bash
cd /opt/zyz-sharehub/deploy
cp .env.prod.example .env.prod
cp .env.staging.example .env.staging
vim .env.prod
vim .env.staging
```

生产 `.env.prod` 必填：

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_REDIRECT_URI`

测试 `.env.staging` 必填：

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_REDIRECT_URI`

对象存储 `DO_SPACES_*` 当前不是启动必填项。

## 5. 启动两套隔离环境

必须显式指定不同的 compose project 名，不能依赖默认目录名：

```bash
cd /opt/zyz-sharehub/deploy

docker compose -p sharehub-prod --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose -p sharehub-staging --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

## 6. 配置系统级 Nginx 统一入口

复制示例：

```bash
cp /opt/zyz-sharehub/deploy/nginx/conf.d/sharehub-ssl.conf.example /etc/nginx/sites-available/sharehub-entry.conf
vim /etc/nginx/sites-available/sharehub-entry.conf
ln -sf /etc/nginx/sites-available/sharehub-entry.conf /etc/nginx/sites-enabled/sharehub-entry.conf
nginx -t
systemctl reload nginx
```

系统级 Nginx 的职责只有两个：

- 接收公网 `80/443`
- 按域名转发到：
  - `127.0.0.1:19080`（prod nginx）
  - `127.0.0.1:19081`（staging nginx）

证书路径默认使用项目内 Certbot 产物：

- `/opt/zyz-sharehub/deploy/certbot/conf/live/your-domain.com/fullchain.pem`
- `/opt/zyz-sharehub/deploy/certbot/conf/live/your-domain.com/privkey.pem`
- `/opt/zyz-sharehub/deploy/certbot/conf/live/staging.your-domain.com/fullchain.pem`
- `/opt/zyz-sharehub/deploy/certbot/conf/live/staging.your-domain.com/privkey.pem`

## 7. 验收命令

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

验收标准：

- 系统级 Nginx 监听 `80/443`
- `prod nginx` 监听 `127.0.0.1:19080`
- `staging nginx` 监听 `127.0.0.1:19081`
- `prod` / `staging` backend 都为 `healthy`
- 两个域名都返回正确健康检查和 GitHub 登录入口

## 8. 更新发布

```bash
cd /opt/zyz-sharehub
git pull

cd /opt/zyz-sharehub/frontend
npm install
npm run build

cd /opt/zyz-sharehub/deploy
docker compose -p sharehub-prod --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose -p sharehub-staging --env-file .env.staging -f docker-compose.staging.yml up -d --build
systemctl reload nginx
```

## 9. 回滚检查项

- 先看 `docker compose ps` 是否某个单独环境失败
- 再看对应环境 `docker compose logs backend`、`docker compose logs nginx`
- 检查系统级 Nginx 转发目标是否还是 `127.0.0.1:19080/19081`
- 检查 `.env.prod` / `.env.staging` 是否误写成同一数据库
- 检查 OAuth App 回调地址是否与域名一致
