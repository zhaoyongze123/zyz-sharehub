# ShareHub 原创资料分享平台

ShareHub 是一个面向学习者和创作者的原创资料分享平台，围绕资料发布、学习路线、笔记沉淀、简历工作台、互动反馈和后台治理构建完整闭环。

项目当前已形成前后端分离架构：

- 后端：Spring Boot 3、PostgreSQL、Redis、Flyway、GitHub OAuth。
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、UnoCSS。
- 部署：Docker Compose、Nginx、DigitalOcean 单机部署模板。
- 测试：Spring Boot 集成测试、Playwright E2E、部署 smoke 脚本。

## 核心功能

### 内容广场

- 原创资料发布、编辑、详情展示。
- 资料分类、标签、关键词检索和热度排序。
- Markdown 内容展示。
- 文件上传与下载。
- 软删除、审核状态、可见性控制。

### 学习路线

- 路线发布与路线详情。
- 路线节点树与附件。
- 用户跟学、进度记录和状态管理。
- 作者工作台与个人路线列表。

### 笔记系统

- 笔记广场。
- 本地笔记编辑。
- 笔记详情、收藏、浏览历史。
- 官方推荐与分类筛选。

### 简历工作台

- 简历列表与详情。
- 多模板简历预览。
- 简历解析与结构化处理。
- PDF 导出能力。

### 用户与互动

- GitHub OAuth 登录。
- 用户资料、头像上传和个人中心聚合。
- 点赞、收藏、评论、举报。
- 用户内容统计。

### 后台治理

- 管理员白名单。
- 举报审核、内容审核、用户管理。
- 审计日志。
- 生产环境禁止依赖 `X-Admin-Token` 作为后台鉴权方式，后台鉴权以 GitHub OAuth 和 PostgreSQL 白名单为准。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 3.3.5 |
| 后端语言 | Java 17 |
| 数据库 | PostgreSQL |
| 缓存 | Redis |
| 数据迁移 | Flyway |
| API 文档 | OpenAPI、springdoc-openapi |
| 前端框架 | Vue 3 |
| 构建工具 | Vite |
| 前端语言 | TypeScript |
| 状态管理 | Pinia |
| 路由 | Vue Router |
| 样式 | Sass、UnoCSS、设计 Token |
| E2E 测试 | Playwright |
| 部署 | Docker Compose、Nginx |

## 目录结构

```text
.
├── backend/                 # Spring Boot 后端服务
├── frontend/                # Vue 3 前端应用
├── data/seeds/              # 资料、路线、笔记种子数据
├── deploy/                  # Docker Compose 与 Nginx 部署配置
├── docs/                    # API、部署、发布和专项文档
├── scripts/                 # 本地联调、导入、部署、自动化脚本
└── README.md
```

## 快速启动

### 1. 环境要求

- JDK 17
- Node.js 20 或兼容版本
- npm
- PostgreSQL
- Redis

### 2. 后端启动

默认后端端口为 `18080`。

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd backend
mvn spring-boot:run
```

如需启用 GitHub OAuth：

```bash
export GITHUB_OAUTH_ENABLED=true
export GITHUB_CLIENT_ID=xxx
export GITHUB_CLIENT_SECRET=xxx
mvn spring-boot:run -Dspring-boot.run.profiles=oauth
```

健康检查：

```bash
curl http://127.0.0.1:18080/actuator/health
```

### 3. 前端启动

默认 Vite 开发服务端口由本地 Vite 配置决定。

```bash
cd frontend
npm install
npm run dev
```

如果需要指定后端代理：

```bash
cd frontend
export VITE_API_PROXY_TARGET='http://127.0.0.1:18080'
npm run dev -- --host 127.0.0.1 --port 14173 --strictPort
```

## 常用脚本

### 后端测试

```bash
cd backend
mvn test
```

### 前端构建

```bash
cd frontend
npm run build
```

### 前端 E2E

```bash
cd frontend
npm run test:e2e
```

### 导入种子数据

```bash
node scripts/import_resources_seed.mjs
node scripts/import_roadmaps_seed.mjs
node scripts/import_notes_seed.mjs
```

种子文件位于：

- `data/seeds/resources.seed.json`
- `data/seeds/roadmaps.seed.json`
- `data/seeds/notes.seed.json`

## API 文档

- OpenAPI 契约：`docs/openapi.yaml`
- 中文后端接口说明：`docs/backend-api-reference.md`
- 本地后端启动后可通过 springdoc 查看接口文档。

统一响应结构：

```json
{
  "success": true,
  "code": "OK",
  "data": {},
  "message": "OK"
}
```

## 部署

部署配置位于 `deploy/`，当前支持生产和 staging 两套 Docker Compose 配置：

- `deploy/docker-compose.prod.yml`
- `deploy/docker-compose.staging.yml`
- `deploy/nginx/`

部署说明：

- `docs/deployment-runbook.md`
- `docs/deploy-digitalocean.md`
- `docs/release-checklist.md`

生产和 staging 建议使用独立 compose project、独立 PostgreSQL、独立 Redis，并由系统级 Nginx 或 Caddy 统一接入公网流量。

## 当前进度

- 已完成标签标准化、内容治理字段与软删除。
- 已完成资料、路线图、笔记的可重复导入种子脚本。
- 已完成内容域 `user_id` 渐进迁移第一阶段。
- 已补充内容查询索引取证脚本 `backend/scripts/explain_content_indexes.sql`。
- 已补充后台专项部署运行手册和夜间自动化推进脚本。

## 开发约定

- `main` 分支保持可部署。
- 功能开发使用 `feature/*`、`fix/*`、`refactor/*`、`test/*` 分支。
- 后端数据结构变更通过 Flyway migration 管理。
- 生产环境以 PostgreSQL 为准，不依赖内存态数据。
- 代码修改后需要运行最小回归测试，并记录真实执行结果。

## License

当前仓库暂未声明开源许可证。如需公开复用，请先补充 `LICENSE` 文件。
