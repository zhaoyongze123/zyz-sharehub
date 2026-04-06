# ShareHub 原创资料分享平台

基于你给出的实施方案落地的首版工程骨架：
- 后端：Spring Boot 3 + GitHub OAuth + 资料/路线/笔记/简历/互动/治理 API 骨架
- 前端：Vue 3 + 设计 Token + component/page contracts（Pixso 高保真约束）
- 部署：DigitalOcean 单机 + Spaces 配置模板

## 快速启动

### 1) 后端
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd backend
mvn spring-boot:run
```

启用 GitHub OAuth 时：
```bash
export GITHUB_OAUTH_ENABLED=true
export GITHUB_CLIENT_ID=xxx
export GITHUB_CLIENT_SECRET=xxx
mvn spring-boot:run -Dspring-boot.run.profiles=oauth
```

### 2) 前端
```bash
cd frontend
npm install
npm run dev
```

## 关键目录
- `backend/`：API 与业务骨架
- `frontend/src/styles/tokens.css`：设计令牌
- `frontend/src/contracts/`：组件契约与页面契约
- `docs/openapi.yaml`：公共 API 约定
- `deploy/`：DigitalOcean 部署模板

## 说明
当前版本实现了可运行骨架与接口契约，便于你在此基础上继续填充真实持久化、审核策略、全文检索与高保真页面。
