# DigitalOcean 单机部署指南（生产）

## 1. 在 DigitalOcean 创建 Droplet
1. 地域：与 Spaces 同区域（建议 `sgp1`）
2. 系统：Ubuntu 22.04 LTS
3. 规格：最低 `2 vCPU / 4GB RAM / 80GB SSD`
4. 网络：放开 22/80/443

## 2. 服务器初始化
```bash
ssh root@<your_server_ip>
apt update && apt upgrade -y
apt install -y ca-certificates curl git

# Docker
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

## 3. 拉取项目
```bash
cd /opt
git clone https://github.com/zhaoyongze123/zyz-sharehub.git
cd zyz-sharehub
```

## 4. 配置生产环境变量
```bash
cd /opt/zyz-sharehub/deploy
cp .env.prod.example .env.prod
vim .env.prod
```

必填项：
- `MYSQL_ROOT_PASSWORD` `MYSQL_PASSWORD` `REDIS_PASSWORD`
- `GITHUB_CLIENT_ID` `GITHUB_CLIENT_SECRET`
- `DO_SPACES_BUCKET` `DO_SPACES_ACCESS_KEY` `DO_SPACES_SECRET_KEY`

## 5. 先以 HTTP 启动（验证服务）
```bash
cd /opt/zyz-sharehub/deploy
docker compose -f docker-compose.prod.yml up -d --build

docker compose -f docker-compose.prod.yml ps
curl http://127.0.0.1/actuator/health
```

预期：返回 `{"status":"UP"}`。

## 6. 配置域名 DNS
在域名服务商将：
- `A your-domain.com -> <your_server_ip>`
- `A www.your-domain.com -> <your_server_ip>`

等待解析生效后执行：
```bash
cd /opt/zyz-sharehub/deploy
mkdir -p certbot/www certbot/conf

docker compose -f docker-compose.prod.yml run --rm --profile certbot certbot \
  certonly --webroot -w /var/www/certbot \
  -d your-domain.com -d www.your-domain.com \
  --email your@email.com --agree-tos --no-eff-email
```

## 7. 启用 HTTPS Nginx 配置
```bash
cd /opt/zyz-sharehub/deploy
cp nginx/conf.d/sharehub-ssl.conf.example nginx/conf.d/sharehub-ssl.conf
vim nginx/conf.d/sharehub-ssl.conf
# 把 your-domain.com 替换成你的真实域名

# 移除默认HTTP配置，避免冲突
mv nginx/conf.d/sharehub.conf nginx/conf.d/sharehub.conf.bak

docker compose -f docker-compose.prod.yml restart nginx
```

## 8. 验收
```bash
curl -I https://your-domain.com
curl https://your-domain.com/actuator/health
```

## 9. 常用运维命令
```bash
cd /opt/zyz-sharehub/deploy

# 查看状态
docker compose -f docker-compose.prod.yml ps

# 查看日志
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f nginx

# 更新发布
cd /opt/zyz-sharehub
git pull
cd deploy
docker compose -f docker-compose.prod.yml up -d --build
```

## 10. 证书续期（建议加 crontab）
```bash
cd /opt/zyz-sharehub/deploy
docker compose -f docker-compose.prod.yml run --rm --profile certbot certbot renew
docker compose -f docker-compose.prod.yml restart nginx
```
