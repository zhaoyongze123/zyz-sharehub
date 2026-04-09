# ShareHub 发布检查清单

## 发布前

- 当前分支代码已 push
- 关键文档已同步：
  - 接口文档
  - 实施文档
  - `docs/deployment-runbook.md`
  - `docs/release-checklist.md`
  - `docs/demo-script.md`
- 后端健康检查通过
- 前端可正常打开
- `module-smoke.spec.ts` 通过
- `full-site-walkthrough.spec.ts` 通过
- 全站走查执行命令已按当前联调地址复核：

```bash
cd /Users/mac/Documents/New\ project/frontend
PLAYWRIGHT_BASE_URL='http://127.0.0.1:14173' \
PLAYWRIGHT_API_BASE_URL='http://127.0.0.1:18080' \
PLAYWRIGHT_USER_KEY='playwright-user' \
PLAYWRIGHT_ADMIN_TOKEN='dev-admin-token' \
npx playwright test tests/e2e/full-site-walkthrough.spec.ts
```

- 若机器上已经有可用前端 dev server，也可把 `PLAYWRIGHT_BASE_URL` 覆盖成对应地址；2026-04-10 07:50 +0800 最近一次复核使用的是默认地址 `http://127.0.0.1:14173`

- 已确认走查账号与权限上下文：
  - 普通用户：`PLAYWRIGHT_USER_KEY`
  - 后台管理员透传头：`PLAYWRIGHT_ADMIN_TOKEN`
- 已知走查会创建真实笔记、资料、路线与节点测试数据，发布前后需允许这类验证写入

## 发布中

- 确认后端已按目标环境变量启动
- 确认前端代理目标正确
- 打开首页、资源、路线、笔记、个人中心、发布页、后台检查核心入口
- 检查飞书通知链路可用

## 发布后

- 检查 `/actuator/health`
- 检查首页可访问
- 检查资源详情页可打开
- 检查路线详情页可打开
- 检查资料发布与路线创建页可完成真实写入
- 检查后台页面权限拦截正常
- 检查后台审计日志页可读取真实记录
- 检查最近一次 Playwright 报告已生成
- 若使用夜间脚本执行，确认报告位于 `output/overnight/*/browser-smoke/playwright-report`

## 回滚条件

- 后端健康检查失败
- 前端首页不可访问
- 资源 / 路线主流程不可用
- 后台权限失效
- 自动化持续异常且无法自愈

## 回滚动作

- 回退到上一稳定提交
- 重启后端与前端
- 重新执行最小 smoke
- 在飞书同步回滚原因和当前状态
