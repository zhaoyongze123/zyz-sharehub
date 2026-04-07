# 后端 API 联调文档

## 1. 文档说明

本文档基于当前 `zyz-sharehub` 后端真实实现整理，用于：

- 前后端联调
- 页面占位与状态设计对齐
- 快速识别“当前已实现能力”和“产品预期但尚未实现能力”

文档依据的真实代码包括：

- [AuthController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/auth/AuthController.java)
- [ResourceController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/resource/ResourceController.java)
- [RoadmapController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/roadmap/RoadmapController.java)
- [NoteController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/note/NoteController.java)
- [ResumeController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/resume/ResumeController.java)
- [InteractionController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/interaction/InteractionController.java)
- [AdminController.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/admin/AdminController.java)
- [SecurityConfig.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/config/SecurityConfig.java)
- [ApiResponse.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/common/ApiResponse.java)
- [InMemoryStore.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/common/InMemoryStore.java)

## 2. 当前实现总览

当前后端实现有这些关键特征：

1. 数据全部保存在内存
   - 当前使用 [InMemoryStore.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/common/InMemoryStore.java)
   - 服务重启后资源、路线、笔记、简历、举报数据都会丢失

2. 多数接口仍是“模拟可联调”状态
   - 接口名称已接近正式业务
   - 但部分行为还未真正落地
   - 例如：发布、点赞、收藏、封禁、下架、简历下载目前都还是占位逻辑

3. 多个详情接口在数据不存在时仍返回 HTTP 200
   - 响应结构可能是 `success=true, data=null, message=OK`
   - 前端不能只依赖 HTTP 状态码判断成功

4. 当前安全策略较宽松
   - `/api/auth/**`
   - `/api/resources/**`
   - `/api/roadmaps/**`
   - `/api/notes/**`
   都被直接放行
   - 后台接口当前也没有真正的管理员鉴权保护

5. 当前文档基线 [openapi.yaml](/Users/mac/Documents/New%20project/docs/openapi.yaml) 只是一份简略接口清单
   - 本文档是更适合联调的详细版

## 3. 通用约定

### 3.1 统一响应结构

来自 [ApiResponse.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/common/ApiResponse.java)：

```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```

失败响应：

```json
{
  "success": false,
  "data": null,
  "message": "错误消息"
}
```

### 3.2 联调注意

1. 不要只看 HTTP 状态码
   - 当前未登录、数据不存在、部分模拟逻辑都可能返回 HTTP 200

2. 前端必须检查：
   - `success`
   - `data`
   - `message`

3. 当前没有统一分页结构
   - 列表接口基本都直接返回完整数组

4. 当前没有统一错误码体系
   - 主要通过 `message` 区分业务失败

## 4. 鉴权与安全配置

安全配置来自 [SecurityConfig.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/config/SecurityConfig.java)。

### 4.1 当前放行路径

- `/actuator/health`
- `/api/auth/**`
- `/api/resources/**`
- `/api/roadmaps/**`
- `/api/notes/**`
- `/oauth2/**`
- `/login/oauth2/**`

### 4.2 OAuth 开关

- `sharehub.auth.github.enabled=false`：不启用 OAuth 登录
- `sharehub.auth.github.enabled=true`：启用 Spring Security OAuth 登录

相关配置文件：

- [application.yml](/Users/mac/Documents/New%20project/backend/src/main/resources/application.yml)
- [application-oauth.yml](/Users/mac/Documents/New%20project/backend/src/main/resources/application-oauth.yml)

### 4.3 当前实现限制

1. `GET /api/auth/me` 未登录时不是标准 401
2. `POST /api/auth/logout` 当前更像“前端确认接口”
3. `/api/admin/**` 当前没有管理员权限校验

## 5. Auth 模块

### 5.1 接口列表

1. `GET /api/auth/github/login`
2. `GET /api/auth/github/callback`
3. `GET /api/auth/me`
4. `POST /api/auth/logout`

### 5.2 `GET /api/auth/github/login`

- 作用：获取 GitHub 登录地址
- 鉴权：无需登录
- 请求参数：无
- 响应：

```json
{
  "success": true,
  "data": {
    "loginUrl": "/oauth2/authorization/github"
  },
  "message": "OK"
}
```

- 联调说明：
  - 前端可以直接跳转 `/oauth2/authorization/github`
  - 或先调用本接口，再读取 `data.loginUrl`

### 5.3 `GET /api/auth/github/callback`

- 作用：说明性接口
- 鉴权：无需登录
- 请求参数：无
- 响应：

```json
{
  "success": true,
  "data": {
    "message": "OAuth callback handled by Spring Security (/login/oauth2/code/github)"
  },
  "message": "OK"
}
```

- 重要说明：
  - 这个接口不是实际 OAuth 回调处理入口
  - 真实回调路径由 Spring Security 处理：`/login/oauth2/code/github`

### 5.4 `GET /api/auth/me`

- 作用：获取当前登录用户
- 鉴权：
  - 路由层放行
  - 业务层通过 `Authentication` 判断是否登录
- 请求参数：无
- 成功响应：

```json
{
  "success": true,
  "data": {
    "name": "Tom",
    "login": "tom-github",
    "avatarUrl": "https://avatars.githubusercontent.com/u/xxx"
  },
  "message": "OK"
}
```

- 未登录响应：

```json
{
  "success": false,
  "data": null,
  "message": "NOT_LOGGED_IN"
}
```

- 当前返回字段：
  - `name`
  - `login`
  - `avatarUrl`

### 5.5 `POST /api/auth/logout`

- 作用：退出登录
- 鉴权：当前无需登录也可调用
- 请求参数：无
- 响应：

```json
{
  "success": true,
  "data": "LOGOUT_SUCCESS",
  "message": "OK"
}
```

### 5.6 Auth 模块差异与风险

1. [openapi.yaml](/Users/mac/Documents/New%20project/docs/openapi.yaml) 缺少：
   - `POST /api/auth/logout`
   - `GET /api/auth/github/callback`
2. `GET /api/auth/me` 未登录返回 200 + `success=false`
3. `GET /api/auth/github/callback` 容易让前端误解为真实回调接口

## 6. Resource 模块

### 6.1 接口列表

1. `POST /api/resources`
2. `GET /api/resources`
3. `GET /api/resources/{id}`
4. `PUT /api/resources/{id}`
5. `DELETE /api/resources/{id}`
6. `POST /api/resources/{id}/publish`

### 6.2 数据结构

来自 [ResourceDto.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/resource/ResourceDto.java)：

```json
{
  "id": 1001,
  "title": "Spring Boot 资料",
  "type": "PDF",
  "summary": "测试",
  "tags": ["Java", "Spring"],
  "externalUrl": "https://example.com",
  "objectKey": "resource/demo.pdf",
  "visibility": "PUBLIC",
  "status": "DRAFT"
}
```

字段说明：

- `title`：必填
- `status`：创建时默认写成 `DRAFT`

### 6.3 `POST /api/resources`

- 作用：创建资料
- 鉴权：开放
- 请求体：`ResourceDto`
- 成功响应：返回新建后的 `ResourceDto`
- 规则：
  - 自动生成 `id`
  - 强制 `status = DRAFT`

### 6.4 `GET /api/resources`

- 作用：获取资料列表
- 鉴权：开放
- 请求参数：无
- 响应：`ApiResponse<List<Object>>`
- 当前行为：
  - 返回全部资料
  - 无分页
  - 无筛选
  - 无排序

### 6.5 `GET /api/resources/{id}`

- 作用：获取资料详情
- 鉴权：开放
- 当前行为：
  - 直接返回内存中的对象
  - 不存在时返回 `success=true, data=null`

### 6.6 `PUT /api/resources/{id}`

- 作用：更新资料
- 鉴权：开放
- 请求体：`ResourceDto`
- 当前行为：
  - 直接按 `id` 覆盖
  - 如果原本不存在，也会写入
  - `status` 为空时重新置为 `DRAFT`

### 6.7 `DELETE /api/resources/{id}`

- 作用：删除资料
- 鉴权：开放
- 响应：

```json
{
  "success": true,
  "data": "DELETED",
  "message": "OK"
}
```

### 6.8 `POST /api/resources/{id}/publish`

- 作用：发布资料
- 鉴权：开放
- 当前行为：
  - 仅回显原对象
  - 不会真正修改 `status`

### 6.9 Resource 模块差异与风险

1. 产品预期有分页/筛选/排序，当前都没有
2. 发布接口名字是“发布”，实现却没有真正发布逻辑
3. 列表和详情返回 `Object`，不是强类型响应
4. 不存在的资料详情仍然是 200

## 7. Roadmap 模块

### 7.1 接口列表

1. `POST /api/roadmaps`
2. `GET /api/roadmaps`
3. `GET /api/roadmaps/{id}`
4. `POST /api/roadmaps/{id}/nodes`
5. `POST /api/roadmaps/{id}/progress`

### 7.2 路线数据结构

来自 [RoadmapDto.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/roadmap/RoadmapDto.java)：

```json
{
  "id": 1001,
  "title": "Agent 工程师 30 天路线",
  "description": "四阶段跑通协议、工具链、工作流与线上监控",
  "visibility": "PUBLIC",
  "status": "PUBLISHED"
}
```

节点结构来自 [RoadmapNodeDto.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/roadmap/RoadmapNodeDto.java)：

```json
{
  "id": 1002,
  "parentId": null,
  "title": "阶段 1：MCP 与工具接入",
  "orderNo": 1,
  "resourceId": 101,
  "noteId": 301
}
```

### 7.3 `POST /api/roadmaps`

- 作用：创建路线
- 鉴权：开放
- 请求体：`RoadmapDto`
- 规则：
  - `title` 必填
  - 自动生成 `id`
  - 创建时强制 `status = PUBLISHED`

### 7.4 `GET /api/roadmaps`

- 作用：路线列表
- 当前行为：
  - 返回全部路线
  - 无分页
  - 无筛选
  - 无排序

### 7.5 `GET /api/roadmaps/{id}`

- 作用：路线详情
- 当前行为：
  - 只返回路线主体
  - 不聚合节点
  - 不聚合进度
  - 不存在时返回 `success=true, data=null`

### 7.6 `POST /api/roadmaps/{id}/nodes`

- 作用：新增路线节点
- 鉴权：开放
- 请求体：`RoadmapNodeDto`
- 当前行为：
  - 节点数据保存在控制器内部 `nodes` Map 中
  - 每次调用都会追加节点
  - 返回值是当前路线下“全部节点列表”，不是单个新增节点

### 7.7 `POST /api/roadmaps/{id}/progress`

- 作用：更新学习进度
- 鉴权：开放
- 请求体：任意 `Map<String, Object>`
- 当前行为：
  - 用整个请求体覆盖保存当前路线进度
  - 不校验结构
  - 没有独立进度查询接口

### 7.8 Roadmap 模块差异与风险

1. 路线详情接口不包含节点和进度
2. 节点接口只有新增，没有查询/删除/更新
3. 进度结构未约定
4. 产品需要的标签筛选、热门/最新排序、作者信息，当前都没有

## 8. Note 模块

### 8.1 接口列表

1. `POST /api/notes`
2. `GET /api/notes`
3. `GET /api/notes/{id}`
4. `PUT /api/notes/{id}`
5. `DELETE /api/notes/{id}`

### 8.2 笔记数据结构

来自 [NoteDto.java](/Users/mac/Documents/New%20project/backend/src/main/java/com/sharehub/note/NoteDto.java)：

```json
{
  "id": 1003,
  "title": "RAG 评测体系复盘",
  "contentMd": "# 标题",
  "visibility": "PUBLIC",
  "status": "PUBLISHED"
}
```

字段说明：

- `title`：必填
- `contentMd`：必填
- `visibility`：当前字段存在，但实现未使用
- `status`：创建时直接写 `PUBLISHED`

### 8.3 `POST /api/notes`

- 作用：创建笔记
- 鉴权：开放
- 请求体：`NoteDto`
- 当前行为：
  - 自动生成 `id`
  - 创建后强制 `status = PUBLISHED`

### 8.4 `GET /api/notes`

- 作用：笔记列表
- 当前行为：
  - 返回全部笔记
  - 无分页
  - 无筛选
  - 无排序

### 8.5 `GET /api/notes/{id}`

- 作用：笔记详情
- 当前行为：
  - 直接返回存储对象
  - 不存在时返回 `success=true, data=null`

### 8.6 `PUT /api/notes/{id}`

- 作用：更新笔记
- 当前行为：
  - 直接覆盖指定 `id`
  - 原本不存在也会写入
  - `status` 由请求体决定

### 8.7 `DELETE /api/notes/{id}`

- 作用：删除笔记
- 响应：

```json
{
  "success": true,
  "data": "DELETED",
  "message": "OK"
}
```

### 8.8 Note 模块差异与风险

1. 当前没有草稿逻辑
2. `visibility` 字段当前不生效
3. 没有创建时间、更新时间等元数据
4. 没有分页、搜索、标签筛选

## 9. Resume 模块

### 9.1 接口列表

1. `POST /api/resumes/generate`
2. `GET /api/resumes/{id}`
3. `GET /api/resumes/{id}/download`

### 9.2 `POST /api/resumes/generate`

- 作用：生成简历
- 鉴权：当前开放
- 请求体：任意 `Map<String, Object>`
- 当前行为：
  - 自动生成 `id`
  - `templateKey` 默认 `default`
  - `status = GENERATED`
  - `fileUrl = /api/resumes/{id}/download`

成功示例：

```json
{
  "success": true,
  "data": {
    "id": 1004,
    "templateKey": "default",
    "status": "GENERATED",
    "fileUrl": "/api/resumes/1004/download"
  },
  "message": "OK"
}
```

### 9.3 `GET /api/resumes/{id}`

- 作用：获取简历详情
- 当前行为：
  - 返回简历元数据
  - 不存在时仍然是 `success=true, data=null`

### 9.4 `GET /api/resumes/{id}/download`

- 作用：下载简历
- 当前实现：
  - 返回占位字符串

示例：

```json
{
  "success": true,
  "data": "PDF_DOWNLOAD_PLACEHOLDER_1004",
  "message": "OK"
}
```

### 9.5 Resume 模块差异与风险

1. 当前不是文件流下载
2. 当前没有真实 PDF 生成
3. 当前只保存极少量元数据，不保存完整简历内容结构

## 10. Interaction 模块

### 10.1 接口列表

1. `POST /api/resources/{id}/comments`
2. `POST /api/comments/{id}/reply`
3. `POST /api/resources/{id}/favorite`
4. `POST /api/resources/{id}/like`
5. `POST /api/reports`

### 10.2 `POST /api/resources/{id}/comments`

- 作用：发表评论
- 鉴权：开放
- 请求体：

```json
{
  "content": "这份资料很有帮助"
}
```

- 响应：

```json
{
  "success": true,
  "data": {
    "resourceId": 101,
    "commentId": 1005,
    "content": "这份资料很有帮助"
  },
  "message": "OK"
}
```

- 当前行为：
  - 不做校验
  - 不持久化评论内容

### 10.3 `POST /api/comments/{id}/reply`

- 作用：回复评论
- 鉴权：开放
- 请求体：`{ "content": "回复内容" }`
- 响应：返回 `parentCommentId`、`commentId`、`content`
- 当前行为：
  - 不持久化

### 10.4 `POST /api/resources/{id}/favorite`

- 作用：收藏资料
- 鉴权：开放
- 请求体：无
- 响应：

```json
{
  "success": true,
  "data": "FAVORITED_101",
  "message": "OK"
}
```

- 当前行为：
  - 仅返回占位字符串
  - 不记录用户收藏状态

### 10.5 `POST /api/resources/{id}/like`

- 作用：点赞资料
- 响应：

```json
{
  "success": true,
  "data": "LIKED_101",
  "message": "OK"
}
```

- 当前行为：
  - 仅返回占位字符串
  - 不记录点赞状态

### 10.6 `POST /api/reports`

- 作用：举报内容
- 鉴权：开放
- 请求体：任意 `Map<String, Object>`
- 当前建议最少字段：

```json
{
  "targetId": 101,
  "reason": "外链失效",
  "contentType": "RESOURCE"
}
```

- 当前行为：
  - 自动生成 `id`
  - 自动写入 `status = OPEN`
  - 存入 `store.reports`

## 11. Admin 模块

### 11.1 接口列表

1. `GET /api/admin/reports`
2. `POST /api/admin/reports/{id}/resolve`
3. `POST /api/admin/resources/{id}/block`
4. `POST /api/admin/users/{id}/ban`

### 11.2 `GET /api/admin/reports`

- 作用：获取举报列表
- 当前行为：
  - 返回 `store.reports` 全量数据
  - 无分页
  - 无筛选

### 11.3 `POST /api/admin/reports/{id}/resolve`

- 作用：处理举报
- 当前行为：
  - 如果找到举报对象且是 `Map`
  - 会把 `status` 更新为 `RESOLVED`
  - 无论存不存在，都返回：

```json
{
  "success": true,
  "data": "RESOLVED_1006",
  "message": "OK"
}
```

### 11.4 `POST /api/admin/resources/{id}/block`

- 作用：下架资料
- 当前行为：
  - 仅返回：

```json
{
  "success": true,
  "data": "BLOCKED_RESOURCE_101",
  "message": "OK"
}
```

- 当前不会真的修改 `store.resources`

### 11.5 `POST /api/admin/users/{id}/ban`

- 作用：封禁用户
- 当前行为：
  - 仅返回：

```json
{
  "success": true,
  "data": "BANNED_USER_88",
  "message": "OK"
}
```

- 当前没有真正的用户状态持久化

### 11.6 Admin 模块差异与风险

1. 当前后台接口没有管理员权限保护
2. 举报处理有状态变更
3. 下架和封禁没有真实状态变更
4. 没有用户列表、审核列表、分类管理等完整后台能力

## 12. 与现有 OpenAPI 草稿不一致的重点

和 [openapi.yaml](/Users/mac/Documents/New%20project/docs/openapi.yaml) 对比，当前最重要的不一致点有：

1. `auth` 模块缺少：
   - `POST /api/auth/logout`
   - `GET /api/auth/github/callback`

2. 多个“详情接口”在数据不存在时返回：
   - HTTP 200
   - `success=true`
   - `data=null`

3. 以下接口名虽然已经稳定，但行为仍是模拟：
   - `POST /api/resources/{id}/publish`
   - `GET /api/resumes/{id}/download`
   - `POST /api/resources/{id}/favorite`
   - `POST /api/resources/{id}/like`
   - `POST /api/admin/resources/{id}/block`
   - `POST /api/admin/users/{id}/ban`

4. 当前没有这些正式产品能力：
   - 列表分页
   - 列表筛选
   - 排序
   - 节点查询/更新/删除
   - 评论查询
   - 草稿逻辑
   - 管理员鉴权

## 13. 前后端联调建议

### 13.1 前端必须额外兜底

前端在联调时必须处理：

1. `success=true` 但 `data=null`
2. 业务动作成功但只是模拟字符串
3. 列表无分页，需要前端本地分页
4. 详情数据不完整，需要前端占位

### 13.2 后端下一阶段优先补齐

建议后端按优先级补：

1. 资料列表分页/筛选/排序
2. 路线详情聚合节点和进度
3. 笔记草稿与可见性逻辑
4. 简历真实 PDF 导出
5. 点赞/收藏/评论持久化
6. 管理员权限控制

### 13.3 文档维护策略

建议保留两层文档：

1. [openapi.yaml](/Users/mac/Documents/New%20project/docs/openapi.yaml)
   - 作为简版路径索引

2. 本文档
   - 作为联调与实现现状说明
   - 重点写清“现在到底实现到了什么程度”
