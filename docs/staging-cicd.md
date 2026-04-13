# 分支驱动 CI/CD 说明

## 标准流程

推荐固定使用以下分支流转：

1. 日常开发在 `feature/*`
2. 验收阶段合并到 `staging`
3. `staging` 自动部署到测试环境
4. `staging` 验证通过后合并到 `main`
5. `main` 自动部署到正式环境

## 自动部署规则

### Staging

当 `staging` 分支发生 `push` 时，GitHub Actions 自动完成以下动作：

1. 拉取仓库代码
2. 构建前端 `dist`
3. 通过 SSH + `rsync` 同步代码到 staging 服务器
4. 写入服务器 `deploy/.env.staging`
5. 执行 `scripts/deploy-staging.sh`
6. 用 `http://127.0.0.1:19081/actuator/health` 做健康检查

### Production

当 `main` 分支发生 `push` 时，GitHub Actions 自动完成以下动作：

1. 拉取仓库代码
2. 构建前端 `dist`
3. 通过 SSH + `rsync` 同步代码到 production 服务器
4. 写入服务器 `deploy/.env.prod`
5. 执行 `scripts/deploy-production.sh`
6. 用 `http://127.0.0.1:19080/actuator/health` 做健康检查

工作流文件：

- `.github/workflows/deploy-staging.yml`
- `.github/workflows/deploy-production.yml`

服务器部署脚本：

- `scripts/deploy-staging.sh`
- `scripts/deploy-production.sh`

## GitHub Secrets

在 GitHub 仓库的 `Settings -> Secrets and variables -> Actions` 中新增以下 secrets：

### staging secrets

| 名称 | 说明 |
| --- | --- |
| `STAGING_HOST` | staging 服务器 IP 或域名 |
| `STAGING_PORT` | SSH 端口，默认可填 `22` |
| `STAGING_USER` | SSH 登录用户，推荐 `root` 或部署专用用户 |
| `STAGING_SSH_KEY` | 私钥全文，建议单独生成 deploy key |
| `STAGING_APP_DIR` | 服务器项目目录，例如 `/opt/zyz-sharehub` |
| `STAGING_ENV_FILE` | 完整 `.env.staging` 文件内容 |

### production secrets

| 名称 | 说明 |
| --- | --- |
| `PROD_HOST` | production 服务器 IP 或域名 |
| `PROD_PORT` | SSH 端口，默认可填 `22` |
| `PROD_USER` | SSH 登录用户，推荐 `root` 或部署专用用户 |
| `PROD_SSH_KEY` | 私钥全文，建议单独生成 deploy key |
| `PROD_APP_DIR` | 服务器项目目录，例如 `/opt/zyz-sharehub` |
| `PROD_ENV_FILE` | 完整 `.env.prod` 文件内容 |

## `STAGING_ENV_FILE` 示例

直接把完整的 `.env.staging` 内容作为一个 secret 存进去，例如：

```dotenv
TZ=Asia/Shanghai
APP_ENV=staging
SPRING_PROFILES_ACTIVE=production,oauth

POSTGRES_DB=sharehub_staging
POSTGRES_USER=sharehub_staging
POSTGRES_PASSWORD=replace_postgres_password

REDIS_PASSWORD=replace_redis_password

SHAREHUB_ADMIN_DEV_TOKEN_ENABLED=false

GITHUB_OAUTH_ENABLED=true
GITHUB_CLIENT_ID=replace_github_test_client_id
GITHUB_CLIENT_SECRET=replace_github_test_client_secret
GITHUB_REDIRECT_URI=https://staging.your-domain.com/login/oauth2/code/github
SHAREHUB_FRONTEND_BASE_URL=https://staging.your-domain.com

STAGING_NGINX_HOST_PORT=19081
```

## 服务器前置条件

服务器需要提前满足：

1. 已安装 Docker 与 Docker Compose Plugin
2. 已创建项目目录，例如 `/opt/zyz-sharehub`
3. 系统级 Nginx / Caddy 已把 staging 域名代理到 `127.0.0.1:19081`
4. GitHub Actions 所用 SSH 私钥拥有该目录写权限

## 触发方式

自动触发：

- push 到 `staging` 分支
- push 到 `main` 分支

手工触发：

- GitHub Actions 页面中运行 `部署 Staging`
- GitHub Actions 页面中运行 `部署 Production`

## 验收标准

staging 部署完成后应满足：

1. `docker compose ps` 中 `sharehub-staging-backend` 为健康状态
2. `curl http://127.0.0.1:19081/actuator/health` 返回 `UP`
3. staging 域名可正常打开前端页面
4. staging GitHub OAuth 回调地址与 `.env.staging` 一致

production 部署完成后应满足：

1. `docker compose ps` 中 `sharehub-backend` 为健康状态
2. `curl http://127.0.0.1:19080/actuator/health` 返回 `UP`
3. 正式域名可正常打开前端页面
4. production GitHub OAuth 回调地址与 `.env.prod` 一致
