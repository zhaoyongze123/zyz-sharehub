# 云端开发专用 PostgreSQL / Redis 联调说明

## 目标

这套配置只服务于“本地启动后端，连接云服务器中间件和数据库”：

- 不影响现网 `sharehub-mysql`
- 不影响现网 `sharehub-redis`
- 不影响现网 `sharehub-backend`
- PostgreSQL 与 Redis 仅绑定服务器 `127.0.0.1`
- 本地通过 SSH 隧道访问

## 当前云服务器开发资源

服务器：

- `159.65.132.59`
- 用户：`root`

开发专用容器：

- PostgreSQL：`sharehub-dev-postgres`
- Redis：`sharehub-dev-redis`

服务器内部绑定端口：

- PostgreSQL：`127.0.0.1:55432`
- Redis：`127.0.0.1:56379`

默认开发库：

- 数据库名：`sharehub_dev`
- 用户名：`sharehub_dev`

## 仓库内对应文件

- Compose 文件：[docker-compose.cloud-dev-services.yml](/Users/mac/Documents/New%20project/deploy/docker-compose.cloud-dev-services.yml)
- SSH 隧道脚本：[sharehub-cloud-dev-tunnel.sh](/Users/mac/Documents/New%20project/scripts/sharehub-cloud-dev-tunnel.sh)

## 在云服务器上启动或重建开发服务

```bash
cd /opt/zyz-sharehub
mkdir -p dev-services
cp deploy/docker-compose.cloud-dev-services.yml dev-services/docker-compose.cloud-dev-services.yml

cd dev-services
export POSTGRES_PASSWORD='请替换为你的开发库密码'
export REDIS_PASSWORD='请替换为你的开发 Redis 密码'
docker compose -f docker-compose.cloud-dev-services.yml up -d
```

检查状态：

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | egrep 'sharehub-dev-postgres|sharehub-dev-redis'
```

## 本地建立 SSH 隧道

```bash
cd /Users/mac/Documents/New\ project
export SSH_PASSWORD='你的云服务器密码'
bash scripts/sharehub-cloud-dev-tunnel.sh
```

建立成功后，本地会拿到：

- `127.0.0.1:55432` -> 云端 PostgreSQL
- `127.0.0.1:56379` -> 云端 Redis

## 本地启动后端

先启用仓库内的 `cloud-dev` profile：

```bash
export SPRING_PROFILES_ACTIVE=cloud-dev
```

## 本地联调建议环境变量

当前仓库的 [application-cloud-dev.yml](/Users/mac/Documents/New%20project/backend/src/main/resources/application-cloud-dev.yml) 读取以下环境变量：

```bash
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=55432
POSTGRES_DB=sharehub_dev
POSTGRES_USER=sharehub_dev
POSTGRES_PASSWORD=请填写开发库密码
REDIS_HOST=127.0.0.1
REDIS_PORT=56379
REDIS_PASSWORD=请填写开发 Redis 密码
```

如果你更习惯直接覆盖 Spring Boot 标准配置，也可以显式传入 `SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*`，但夜间联调默认建议使用上面的 `POSTGRES_*` / `REDIS_*` 组合，和仓库配置保持一致。

## 验证命令

PostgreSQL：

```bash
PGPASSWORD='请填写开发库密码' psql -h 127.0.0.1 -p 55432 -U sharehub_dev -d sharehub_dev -c '\conninfo'
```

Redis：

```bash
redis-cli -h 127.0.0.1 -p 56379 -a '请填写开发 Redis 密码' ping
```

## 已完成的服务器侧实际落地

已在云服务器执行：

- 创建开发专用 compose 目录：`/opt/zyz-sharehub/dev-services`
- 启动 `sharehub-dev-postgres`
- 启动 `sharehub-dev-redis`
- 绑定端口到 `127.0.0.1`

## 风险说明

- 当前 SSH 免密登录尚未稳定验证，现阶段脚本默认使用 `sshpass + 密码`
- 仓库中不保存任何真实密码，需要本地环境变量注入
- 当前项目现网仍在使用 MySQL，开发用 PostgreSQL 是并行独立环境
