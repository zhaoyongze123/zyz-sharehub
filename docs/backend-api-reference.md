# ShareHub 后端 API 参考

## 1. 文档范围

本文件基于当前 `backend` 实现与集成测试整理，目标是补充 `docs/openapi.yaml` 的中文联调说明。若文档与代码冲突，以当前后端实现和测试结果为准。

仓库基线：

- 后端目录：`backend`
- 当前技术栈：`Spring Boot 3 + PostgreSQL + Redis + Flyway`
- 文件存储：小文件直接入 `PostgreSQL`
- 联调方式：支持 OAuth 会话，也支持本地开发头透传

## 2. 通用规则

### 2.1 响应结构

除下载接口外，统一返回：

```json
{
  "success": true,
  "code": "OK",
  "data": {},
  "message": "OK"
}
```

错误响应统一返回：

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "data": null,
  "message": "ERROR_CODE"
}
```

### 2.2 分页结构

统一分页字段为：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0
}
```

注意：

- `resources` 列表当前是 **0 基页码**
- `me/*`、`notes`、`resumes`、`roadmaps`、`admin/*` 当前是 **1 基页码**

### 2.3 联调请求头

普通用户联调：

- `X-User-Key: <user-key>`

管理员联调：

- `X-Admin-Token: <admin-token>`

当前管理员鉴权真实行为：

- 缺 token、空 token、错误 token 都返回 `403`
- 当前没有单独的 `401` 分支

### 2.4 文件上传规则

统一使用 `multipart/form-data`。

当前真实实现已校验：

- 文件非空
- 文件名存在
- 引用归属存在
- 大小不超过 `5MB`

当前真实实现未校验：

- MIME 白名单
- 扩展名白名单

## 3. 认证与用户态

### 3.1 `GET /api/auth/github/login`

返回 GitHub OAuth 登录入口：

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "loginUrl": "/oauth2/authorization/github"
  },
  "message": "OK"
}
```

### 3.2 `GET /api/auth/github/callback`

仅返回联调说明，真实 OAuth 回调仍由 Spring Security 处理：

- 实际回调路径：`/login/oauth2/code/github`

### 3.3 `GET /api/auth/me`

要求：

- OAuth 登录态，或
- `X-User-Key`

失败状态：

- `401`：未登录
- `403`：用户被封禁，错误码 `USER_BANNED`

### 3.4 `POST /api/auth/avatar`

要求：

- OAuth 登录态，或
- `X-User-Key`

真实行为：

- 使用 `multipart/form-data`
- 必填参数：`file`
- 当前复用文件存储能力，文件 `owner` 取当前登录用户，`category=AVATAR`，`referenceType=USER_AVATAR`，`referenceId` 为当前用户标识
- 校验文件非空、文件名存在、大小不超过 `5MB`
- 成功后返回文件元数据，`downloadUrl` 形如 `/api/files/{uuid}`，返回体中的 `contentType` 始终有值；上传缺省或非法时回落为 `application/octet-stream`
- 成功后把头像文件 `id` 回写到用户资料

失败状态：

- `400`：文件为空、缺失文件名、超过 5MB 等
- `401`：未登录
- `403`：用户被封禁

## 4. 个人中心

- 联调时可通过 `X-User-Key` 指定当前用户；若该用户首次访问个人中心相关接口，服务端会自动补建一条默认用户档案

### 4.1 `GET /api/me`

返回用户聚合信息，包括：

- 用户资料
- 资料数
- 收藏数
- 路线数
- 笔记数
- 简历数
- 最近 7 天新增资料数
- 已发布资料数
- 草稿笔记数
- 已生成简历数

真实边界：

- 需要 OAuth 登录态，或本地联调头 `X-User-Key`
- 首次访问会自动补建一条默认用户档案
- 未带身份时返回 `401 NOT_LOGGED_IN`
- 用户已被封禁时返回 `403 USER_BANNED`

### 4.2 列表化接口

已实现：

- `GET /api/me/resources`
- `GET /api/me/roadmaps`
- `GET /api/me/favorites`
- `GET /api/me/notes`
- `GET /api/me/resumes`

都要求：

- OAuth 登录态，或
- `X-User-Key`

当前支持的筛选：

- `my resources`: `status`、`visibility`
- `my roadmaps`: `status`
- `my notes`: `status`
- `my resumes`: `status`、`templateKey`、`keyword`

补充说明：

- `GET /api/me/resources` 返回的是当前用户自己的资料工作台列表，不是仅“已发布资料”
- 默认不额外收窄 `status`，因此会同时返回该用户的草稿 / 已发布资料；如需只看已发布数据，需要显式传 `status=PUBLISHED`
- 这些列表接口首次访问时都会自动补建默认用户档案
- 这些列表接口未带身份时统一返回 `401 NOT_LOGGED_IN`
- 这些列表接口用户被封禁时统一返回 `403 USER_BANNED`
- 这些列表接口的 `page`、`pageSize` 传入小于 `1` 的值时，都会按 `1` 兜底
- `GET /api/me/resources` 的 `status`、`visibility` 为空字符串或仅空白时，按未传处理
- `GET /api/me/resources` 的 `status`、`visibility` 若带前后空白，会先裁剪再参与筛选
- `GET /api/me/roadmaps` 的 `status` 为空字符串或仅空白时，按未传处理；若带前后空白，会先裁剪再参与筛选
- `GET /api/me/notes` 的 `status` 为空字符串或仅空白时，按未传处理，不额外收窄结果；若带前后空白，会先裁剪再参与筛选
- `GET /api/me/resumes` 的 `status`、`templateKey`、`keyword` 为空字符串或仅空白时，按未传处理；若带前后空白，会先裁剪再参与筛选
- `GET /api/me/resumes` 列表项会带出简历文件元数据，重点字段包括 `fileUrl`、`fileName`、`fileSize`、`fileCreatedAt`、`fileUpdatedAt`

## 5. 资料模块

### 5.1 `GET /api/resources`

当前支持：

- `page`
- `pageSize`
- `keyword`
- `status`
- `visibility`
- `category`
- `tag`
- `sortBy`

真实行为：

- 未传 `status` 时，只返回 `PUBLISHED`
- `status` 为空字符串、仅空白，或裁剪后没有有效状态项时，按未传处理
- `status` 带前后空白时会先裁剪再参与筛选
- `category` 当前映射到资源表的 `type`
- `sortBy=latest` 按 `updatedAt DESC`
- `sortBy=hot` 按点赞数降序，再按更新时间降序
- 默认按 `updatedAt DESC`

### 5.2 `GET /api/resources/featured`

返回最近更新的 6 条已发布资料。

### 5.3 `GET /api/resources/{id}`

真实状态语义：

- `200`：成功
- `404`：不存在，错误码 `NOT_FOUND`
- `410`：已下架，错误码 `RESOURCE_REMOVED`

当前详情 / 列表返回还包含这些展示字段：

- `category`
- `updatedAt`
- `author`
- `likes`
- `favorites`
- `downloadCount`

### 5.4 `POST /api/resources`

当前真实行为：

- 统一通过 `RequestAccessService` 解析当前用户
- 联调环境可用 `X-User-Key` 指定用户；接入 OAuth 后会优先取登录态
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- `status` 固定初始化为 `DRAFT`

### 5.5 `PUT /api/resources/{id}`

当前真实行为：

- 只允许当前 owner 更新自己的资料
- 非 owner 返回 `403 RESOURCE_FORBIDDEN`
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 不存在返回 `404 NOT_FOUND`

### 5.6 `DELETE /api/resources/{id}`

当前真实行为：

- 只允许当前 owner 删除自己的资料
- 非 owner 返回 `403 RESOURCE_FORBIDDEN`
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 不存在返回 `404 RESOURCE_NOT_FOUND`

### 5.7 `POST /api/resources/{id}/publish`

真实行为：

- 只允许当前 owner 发布自己的资料
- 非 owner 返回 `403 RESOURCE_FORBIDDEN`
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 资料状态改为 `PUBLISHED`
- 不存在返回 `404 NOT_FOUND`

### 5.8 `POST /api/resources/{id}/attachment`

真实行为：

- 只允许当前 owner 上传自己的资料附件
- 非 owner 返回 `403 RESOURCE_FORBIDDEN`
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 资料不存在返回 `404 RESOURCE_NOT_FOUND`
- 文件校验失败时返回 `400`，当前已覆盖 `FILE_NAME_REQUIRED`、`FILE_EMPTY`、`FILE_TOO_LARGE`
- 文件进入 PostgreSQL 文件表
- 成功后把 `objectKey` 更新为文件 `id`
- 返回体包含 `resourceId` 和 `file` 两部分，其中 `file.downloadUrl` 形如 `/api/files/{uuid}`
- 返回体中的 `file.contentType` 始终有值；上传缺省或非法时回落为 `application/octet-stream`

### 5.9 `GET /api/resources/{id}/related`

真实行为：

- 基于同分类或标签交集做轻量推荐
- 只返回 `PUBLISHED`
- 排除自身
- 最多返回 `4` 条

## 6. 路线模块

已实现：

- `GET /api/roadmaps`
- `POST /api/roadmaps`
- `GET /api/roadmaps/{id}`
- `POST /api/roadmaps/{id}/nodes`
- `POST /api/roadmaps/{id}/nodes/{nodeId}/attachments`
- `POST /api/roadmaps/{id}/progress`

当前状态：

- 创建、节点新增、进度更新统一通过 `RequestAccessService` 解析当前用户
- 联调环境可用 `X-User-Key` 指定用户；接入 OAuth 后会优先取登录态
- `POST /api/roadmaps`、`POST /api/roadmaps/{id}/nodes`、`POST /api/roadmaps/{id}/progress` 未带用户身份时统一返回 `401 NOT_LOGGED_IN`
- 上述 3 个写接口在请求用户已被封禁时统一返回 `403 USER_BANNED`
- `POST /api/roadmaps` 创建后的 owner 为当前请求用户
- `POST /api/roadmaps/{id}/nodes`、`POST /api/roadmaps/{id}/nodes/{nodeId}/attachments`、`POST /api/roadmaps/{id}/progress` 只允许当前 owner 操作；非 owner 按 `404 ROADMAP_NOT_FOUND` 返回
- `GET /api/roadmaps/{id}` 的 `progress` 按当前请求用户读取；未登录、未写入进度或切换到其他用户时返回空对象；如果请求里显式带了已封禁用户，则返回 `403 USER_BANNED`
- `GET /api/roadmaps/{id}` 仅在路线不存在时返回 `404 ROADMAP_NOT_FOUND`
- 节点标题、描述、顺序与进度已持久化
- 节点附件通过 `files` 表持久化，按 `referenceType=ROADMAP_NODE` 与 `category=ROADMAP_NODE_ATTACHMENT` 绑定
- 列表筛选仍较弱，暂未覆盖前端清单中的标签筛选

### 6.1 `POST /api/roadmaps/{id}/nodes/{nodeId}/attachments`

真实行为：

- 需要当前用户身份
- 只允许路线 owner 上传节点附件
- 使用 `multipart/form-data`
- 成功后返回附件元数据，包含 `downloadUrl`
- 文件内容直接写入 `PostgreSQL files`

## 7. 文件存储模块

已实现：

- `POST /api/files/upload`
- `GET /api/files/{id}`

当前真实边界：

- 文件内容直接写入 `PostgreSQL`
- 这组接口当前不走登录鉴权
- 业务侧通常通过头像上传、资料附件上传、简历生成间接写入，不建议前端业务直接依赖直传接口

### 7.1 `POST /api/files/upload`

真实行为：

- 使用 `multipart/form-data`
- 必填参数：`owner`、`category`、`referenceType`、`referenceId`、`file`
- `category` 当前真实枚举：`AVATAR`、`RESOURCE_ATTACHMENT`、`ROADMAP_NODE_ATTACHMENT`、`RESUME_PDF`
- 校验文件非空、文件名存在、引用归属存在、大小不超过 `5MB`
- 成功后返回文件元数据，`downloadUrl` 形如 `/api/files/{uuid}`，返回体中的 `contentType` 始终有值；上传缺省或非法时回落为 `application/octet-stream`

失败状态：

- `400 FILE_OWNER_REQUIRED`
- `400 FILE_REFERENCE_REQUIRED`
- `400 FILE_NAME_REQUIRED`
- `400 FILE_EMPTY`
- `400 FILE_TOO_LARGE`
- `400 FILE_READ_ERROR`

### 7.2 `GET /api/files/{id}`

真实行为：

- 按文件 `id` 直接返回二进制内容
- `Content-Type` 优先使用入库时的 `contentType`，若为空、`NULL` 或非法则回落到 `application/octet-stream`
- `Content-Disposition` 固定为 `attachment; filename="{原始文件名}"`
- 文件不存在时返回空 body 的 `404`

## 8. 笔记模块

已实现：

- `GET /api/notes`
- `POST /api/notes`
- `GET /api/notes/{id}`
- `PUT /api/notes/{id}`
- `DELETE /api/notes/{id}`

真实状态：

- 全部接口统一通过 `RequestAccessService` 解析当前用户
- 联调环境可用 `X-User-Key` 指定用户；接入 OAuth 后会优先取登录态
- `GET /api/notes`、`PUT /api/notes/{id}`、`DELETE /api/notes/{id}` 只作用于当前用户自己的笔记
- `GET /api/notes/{id}` 在笔记为 `visibility=PUBLIC` 且 `status=PUBLISHED` 时允许匿名或其他登录用户直接读取
- `GET /api/notes` 支持 `status` 筛选；当 `status` 为空字符串或仅空白时按未传处理
- `GET /api/notes` 的 `status` 若带前后空白，会先裁剪再参与筛选
- `POST /api/notes` 未传 `status`、传 `null`、空字符串或仅空白时默认存为 `DRAFT`
- `POST /api/notes`、`PUT /api/notes/{id}` 的 `visibility` 传空字符串或仅空白时按未传处理，最终存为 `null`
- `PUT /api/notes/{id}` 传空字符串或仅空白 `status` 时按未传处理，保留原状态
- 非公开已发布笔记对他人和匿名访问统一按不存在返回 `404 NOTE_NOT_FOUND`
- 未带用户身份访问列表、创建、更新、删除时返回 `401 NOT_LOGGED_IN`
- `404` 统一为 `NOTE_NOT_FOUND`
- 用户已被封禁时返回 `403 USER_BANNED`

当前重要实现边界：

- `GET /api/notes` 当前支持 `status`、`page`、`pageSize`
- `page`、`pageSize` 传入小于 `1` 的值时都会按 `1` 兜底
- 后端仍保存 `content_md` 正文
- 这与“正文只保存在用户本地”的产品目标不一致
- 该模块后续仍需要专项重构

## 9. 简历模块

已实现：

- `POST /api/resumes/generate`
- `GET /api/resumes`
- `GET /api/resumes/{id}`
- `DELETE /api/resumes/{id}`
- `GET /api/resumes/{id}/download`
- `GET /api/resumes/workbench`

当前真实边界：

- 全部接口统一通过 `RequestAccessService` 解析当前用户
- 联调环境可用 `X-User-Key` 指定用户；接入 OAuth 后会优先取登录态
- 未带用户身份时返回 `401 NOT_LOGGED_IN`

### 9.1 生成接口

`POST /api/resumes/generate`

真实行为：

- 根据 `templateKey` 生成简单 PDF
- `templateKey` 未传、为 `null`、空字符串或仅空白时统一回落为 `default`
- PDF 入 PostgreSQL 文件表
- 再创建简历记录
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 用户已被封禁时返回 `403 USER_BANNED`

### 9.2 列表接口

`GET /api/resumes`

当前支持：

- `status`
- `templateKey`
- `keyword`
- `page`
- `pageSize`
- `status`、`templateKey`、`keyword` 传空字符串或仅空白时按未传处理
- `status`、`templateKey`、`keyword` 若带前后空白，会先裁剪再参与筛选
- `page`、`pageSize` 传入小于 `1` 的值时都会按 `1` 兜底
- 用户已被封禁时返回 `403 USER_BANNED`

真实返回字段重点：

- `fileUrl`：实际下载链接 `/api/resumes/{id}/download`
- `fileName`
- `fileSize`
- `fileCreatedAt`
- `fileUpdatedAt`

### 9.3 详情 / 删除 / 下载

`GET /api/resumes/{id}`

- 只允许访问当前用户拥有的简历
- 用户已被封禁时返回 `403 USER_BANNED`
- 不存在或非 owner 统一返回 `404 RESUME_NOT_FOUND`

`DELETE /api/resumes/{id}`

- 只允许删除当前用户拥有的简历
- 删除时会同步清理关联文件记录
- 用户已被封禁时返回 `403 USER_BANNED`
- 不存在或非 owner 统一返回 `404 RESUME_NOT_FOUND`

`GET /api/resumes/{id}/download`

- 只允许下载当前用户拥有的简历
- 用户已被封禁时返回 `403 USER_BANNED`
- 简历不存在或非 owner 返回 `404 RESUME_NOT_FOUND`
- 若文件记录已丢失，当前实现返回空 body 的 `404`
- `Content-Type` 优先使用文件记录中的 `contentType`；若为空、`NULL` 或非法则回落为 `application/octet-stream`
- 当简历不存在或非 owner 时，错误响应体为 `application/json`，结构同 `ErrorResponse`（`success=false, code, message`），示例：
  - `{ success:false, code:"RESUME_NOT_FOUND", message:"RESUME_NOT_FOUND" }`
- 当底层文件记录已丢失时，仅返回空 body 的 `404`
- 响应头：`Content-Disposition` 为 `attachment; filename="{原始文件名}"`

`GET /api/resumes/workbench`

- 按当前用户聚合简历统计
- `templateBreakdown` 按数量倒序返回；数量相同时按 `templateKey` 升序稳定排序
- `recentItems` 按创建时间倒序返回；时间相同时按 `id` 倒序稳定排序
- `recentItems` 元素结构与简历列表项保持一致，包含 `fileUrl`、`fileName`、`fileSize`、`fileCreatedAt`、`fileUpdatedAt`
- 未带用户身份时返回 `401 NOT_LOGGED_IN`
- 用户已被封禁时返回 `403 USER_BANNED`

## 10. 互动模块

已实现：

- `GET /api/resources/{id}/comments`
- `POST /api/resources/{id}/comments`
- `POST /api/comments/{id}/reply`
- `POST /api/resources/{id}/favorite`
- `DELETE /api/resources/{id}/favorite`
- `POST /api/resources/{id}/like`
- `DELETE /api/resources/{id}/like`
- `GET /api/resources/{id}/interactions`
- `GET /api/notes/{id}/interactions`
- `POST /api/notes/{id}/favorite`
- `DELETE /api/notes/{id}/favorite`
- `POST /api/notes/{id}/like`
- `DELETE /api/notes/{id}/like`
- `POST /api/reports`

当前边界：

- 评论、收藏、点赞主要围绕 `RESOURCE`
- 笔记互动统计、收藏、点赞、举报按笔记详情同样的可见性规则校验：owner 可访问自己的任意笔记，其他用户和匿名只允许访问 `PUBLIC + PUBLISHED`
- 举报支持 `RESOURCE` 与 `NOTE`
- 还没有真正抽象成多内容类型互动模型
- 上述互动写接口在未登录时返回 `401/NOT_LOGGED_IN`，用户被封禁时返回 `403/USER_BANNED`，举报目标不存在或当前访问者不可见时返回 `404/RESOURCE_NOT_FOUND` 或 `404/NOTE_NOT_FOUND`
- `POST /api/resources/{id}/comments` 和 `POST /api/comments/{id}/reply` 在 `content` 缺失、空字符串或仅空白字符时返回 `400 COMMENT_CONTENT_REQUIRED`
- `POST /api/reports` 的 `reason` 缺失、空字符串或仅空白时回落为 `无`；资源举报传 `resourceId`，笔记举报传 `targetType=NOTE` + `noteId`

## 11. 后台治理

已实现：

- `GET /api/admin/reports`
- `GET /api/admin/audit-logs`
- `GET /api/admin/users`
- `POST /api/admin/reports/{id}/resolve`
- `POST /api/admin/resources/{id}/block`
- `POST /api/admin/resources/{id}/restore`
- `POST /api/admin/users/{id}/ban`
- `POST /api/admin/users/{id}/unban`
- `POST /api/admin/comments/{id}/hide`
- `POST /api/admin/comments/{id}/restore`

真实鉴权规则：

- 统一依赖 `X-Admin-Token`
- 当前失败统一返回 `403`

## 12. 当前已知偏差

以下内容已经由代码证实，后续需要继续收口：

1. `docs/backend-api-reference.md` 此次补齐前原本不存在。
2. `resumes` 已切到按请求用户隔离，但仍依赖 `X-User-Key` / OAuth 上下文，不是完整业务登录流。
3. `resources` 写接口已经切到按请求用户隔离，联调依赖 `X-User-Key` / OAuth 上下文，不是完整业务登录流。
4. `auth/avatar` 旧文档承诺了图片类型白名单，但当前实现并没有该校验。

## 13. 推荐联调顺序

1. 先使用 `X-User-Key` 打通 `auth/me` 和 `me`
2. 再接 `roadmaps`
3. 最后处理 `notes` 的本地存储重构
