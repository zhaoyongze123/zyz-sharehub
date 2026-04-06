# Pixso MCP 高保真协作流程

## 1. 设计先行
1. 在 Pixso 建立页面全集：首页、资料列表、资料详情、发布、路线、笔记、简历。
2. 同步输出三类契约：
- design-tokens
- component-contracts
- page-contracts

## 2. 开发落地
1. 先更新 `frontend/src/contracts/*.json`。
2. 再更新 `frontend/src/styles/tokens.css`。
3. 页面只允许消费 token，不允许硬编码色值与间距。

## 3. 对照验收
1. 每页交付时执行 `docs/pixso-acceptance-checklist.md`。
2. 运行 `./scripts/verify_tokens.sh` 保证颜色未硬编码。
3. 桌面与移动端分别截图进行 Pixso 对照。

## 4. 发布门禁
- 功能测试通过（后端接口 + 前端构建）
- 视觉验收完成（结构/视觉/交互）
- Token 校验通过
