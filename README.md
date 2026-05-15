# 📚 ShareHub - 原创资料分享平台

[![GitHub stars](https://img.shields.io/github/stars/zhaoyongze123/zyz-sharehub?style=social)](https://github.com/zhaoyongze123/zyz-sharehub/stargazers)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-orange.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

> 🔥 **在线访问**: [https://zyzsharehub.cn](https://zyzsharehub.cn)

ShareHub 是一个面向学习者和创作者的**原创资料分享平台**，围绕资料发布、学习路线、笔记沉淀、简历工作台、互动反馈和后台治理构建完整闭环。

---

## ✨ 功能特色

| 模块 | 说明 |
|------|------|
| 📖 **内容广场** | 原创资料发布、分类检索、标签筛选、热度排序、Markdown 展示、文件上传下载 |
| 🛤️ **学习路线** | 路线发布、节点树管理、用户跟学、进度记录 |
| 📝 **笔记系统** | 笔记广场、本地编辑、收藏浏览、官方推荐 |
| 📄 **简历工作台** | 多模板简历预览、智能解析、PDF 导出 |
| ❤️ **用户互动** | GitHub OAuth 登录、点赞收藏评论举报 |
| ⚙️ **后台治理** | 审核管理、用户管理、审计日志 |

---

## 🖼️ 截图预览

| 首页 | 资料详情 |
|:---:|:---:|
| ![首页](docs/screenshots/home.png) | ![资料详情](docs/screenshots/resource-detail.png) |

| 学习路线 | 笔记广场 |
|:---:|:---:|
| ![学习路线](docs/screenshots/roadmap.png) | ![笔记广场](docs/screenshots/notes.png) |

---

## 🛠️ 技术栈

### 后端
<p>
  <img src="https://img.shields.io/badge/Java-17-red.svg?style=flat-square&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg?style=flat-square&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-7-red.svg?style=flat-square&logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/Flyway-Migration-yellow.svg?style=flat-square" alt="Flyway">
</p>

### 前端
<p>
  <img src="https://img.shields.io/badge/Vue-3-44AAFF.svg?style=flat-square&logo=vue.js" alt="Vue 3">
  <img src="https://img.shields.io/badge/Vite-5-646CFF.svg?style=flat-square&logo=vite" alt="Vite">
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6.svg?style=flat-square&logo=typescript" alt="TypeScript">
  <img src="https://img.shields.io/badge/Pinia-2-FFD859.svg?style=flat-square" alt="Pinia">
  <img src="https://img.shields.io/badge/UnoCSS-Active-333333.svg?style=flat-square" alt="UnoCSS">
</p>

---

## 📁 项目结构

```
.
├── backend/                 # Spring Boot 后端服务
├── frontend/                # Vue 3 前端应用
├── data/seeds/              # 资料、路线、笔记种子数据
├── deploy/                  # Docker Compose 与 Nginx 部署配置
├── docs/                    # API、部署、发布和专项文档
│   └── screenshots/         # 项目截图
├── scripts/                 # 本地联调、导入、部署、自动化脚本
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- ☕ JDK 17+
- 📦 Node.js 20+ / npm
- 🐘 PostgreSQL 16+
- 🗃️ Redis 7+

### 后端启动

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd backend
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 导入种子数据

```bash
node scripts/import_resources_seed.mjs
node scripts/import_roadmaps_seed.mjs
node scripts/import_notes_seed.mjs
```

---

## 📖 API 文档

- 📄 [OpenAPI 契约](docs/openapi.yaml)
- 📘 [后端接口参考](docs/backend-api-reference.md)

> 💡 本地启动后可通过 `springdoc` 查看交互式 API 文档

---

## 🚢 部署

部署配置位于 `deploy/`：

| 环境 | 配置文件 |
|------|----------|
| 🏭 生产环境 | `docker-compose.prod.yml` |
| 🧪 测试环境 | `docker-compose.staging.yml` |

详细部署文档：
- [部署运行手册](docs/deployment-runbook.md)
- [DigitalOcean 部署指南](docs/deploy-digitalocean.md)
- [发布检查清单](docs/release-checklist.md)

---

## 👨‍💻 关于作者

<div align="center">

### 🔗 联系方式

[![GitHub](https://img.shields.io/badge/GitHub-zhaoyongze123-333.svg?style=flat-square&logo=github)](https://github.com/zhaoyongze123)
[![Email](https://img.shields.io/badge/Email-zhaoyongze@email.com-EA4335.svg?style=flat-square&logo=gmail)](mailto:zhaoyongze@email.com)

### 🏆 统计数据

![Stats](https://github-readme-stats.vercel.app/api?username=zhaoyongze123&theme=radical&hide_border=false)

![Top Langs](https://github-readme-stats.vercel.app/api/top-langs/?username=zhaoyongze123&theme=radical&hide_border=false)

</div>

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 License

本项目采用 [MIT License](LICENSE) 开源。