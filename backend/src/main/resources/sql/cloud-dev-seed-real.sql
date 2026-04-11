BEGIN;

INSERT INTO users (id, login, name, bio, preferences_json, avatar_file_id, status, created_at, updated_at)
VALUES
  (1, 'liang-zhou', '周亮', '后端工程师，长期做 Agent 编排、RAG 可观测性与发布稳定性治理。', '{"theme":"system","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (2, 'yan-lin', '林妍', '前端工程师，专注真实接口闭环、可访问性与浏览器验收。', '{"theme":"light","weeklyDigest":true,"showEmail":true}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '58 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (3, 'hao-wu', '吴昊', '平台工程师，负责 PostgreSQL、Redis、Flyway 和发布流水线。', '{"theme":"dark","weeklyDigest":false,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '56 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
  (4, 'jia-chen', '陈嘉', '社区作者，持续产出 Agent/RAG 实战笔记与评测复盘。', '{"theme":"system","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '54 days', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
  (5, 'qi-song', '宋祺', '全栈开发，维护路线内容与资料库质量。', '{"theme":"system","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '53 days', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
  (6, 'admin-zoe', 'Admin Zoe', '平台管理员，负责举报治理、内容审核与审计追踪。', '{"theme":"system","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
  (7, 'ming-xu', '徐明', 'AI 平台工程师，聚焦模型路由、熔断与压测。', '{"theme":"dark","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
  (8, 'na-zheng', '郑娜', '数据工程师，负责特征清洗和评测数据闭环。', '{"theme":"system","weeklyDigest":true,"showEmail":true}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '44 days', CURRENT_TIMESTAMP - INTERVAL '5 hours'),
  (9, 'kai-liu', '刘凯', 'SRE，关注发布稳定性、告警分级和应急预案。', '{"theme":"light","weeklyDigest":false,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '43 days', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
  (10, 'ting-he', '何婷', '技术写作者，擅长把工程方案沉淀为可复现文档。', '{"theme":"system","weeklyDigest":true,"showEmail":true}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '42 days', CURRENT_TIMESTAMP - INTERVAL '9 hours'),
  (11, 'rui-fan', '范睿', '算法工程师，主攻检索评测与重排优化。', '{"theme":"dark","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '41 days', CURRENT_TIMESTAMP - INTERVAL '11 hours'),
  (12, 'wen-yao', '姚雯', '后端工程师，负责账户权限与审计链路。', '{"theme":"system","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (13, 'bo-jiang', '蒋博', '测试开发，维护 Playwright 与 API 自动化基线。', '{"theme":"light","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '39 days', CURRENT_TIMESTAMP - INTERVAL '4 hours'),
  (14, 'xue-an', '安雪', '产品工程师，负责学习路线内容规划与质量追踪。', '{"theme":"system","weeklyDigest":true,"showEmail":true}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '38 days', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
  (15, 'chen-lei', '雷辰', '全栈工程师，擅长前后端协同与可观测性接入。', '{"theme":"dark","weeklyDigest":true,"showEmail":false}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '37 days', CURRENT_TIMESTAMP - INTERVAL '13 hours'),
  (16, 'su-yu', '于苏', '社区运营，负责内容治理和精选推荐。', '{"theme":"system","weeklyDigest":true,"showEmail":true}', NULL, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '36 days', CURRENT_TIMESTAMP - INTERVAL '6 hours');

INSERT INTO resources (
  id, title, type, summary, tags, external_url, object_key, owner_key, visibility, status, created_at, updated_at
)
VALUES
  (1, 'LangGraph 官方仓库与多代理状态流最佳实践', 'REPO',
   '## 背景\nLangGraph 是当前业界常见的有状态 Agent 编排底座。\n\n## 你会学到\n1. 如何把长链路任务拆成 graph 节点。\n2. 如何用 checkpoint 保障中断恢复。\n3. 如何做 human-in-the-loop 的可追溯介入。\n\n## 实操建议\n- 节点输入输出保持 schema 稳定。\n- 把大对象移出状态，仅保留引用键。\n- 每次发布前做一次故障注入演练。\n\n## 可复现步骤\n1. 拉取官方示例并跑通本地最小链路。\n2. 注入超时和网络故障验证恢复逻辑。\n3. 对比恢复前后任务成功率与耗时。\n\n## 结论\n这份资料适合做生产级 Agent 编排的第一块基石。',
   'langgraph,agent,orchestration,python',
   'https://github.com/langchain-ai/langgraph',
   'attachments/resource-1/langgraph-production-checklist.pdf',
   'liang-zhou', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
  (2, 'Microsoft AutoGen 多智能体协作指南', 'REPO',
   '## 背景\nAutoGen 适合快速搭建多智能体协作流程。\n\n## 重点\n- 角色拆分与任务分工\n- 工具调用授权边界\n- 会话终止条件\n\n## 经验\n上线前务必限制最大回合与工具次数，避免成本不可控。\n\n## 演练清单\n- 正常路径\n- 工具失败重试\n- 上下文超长降级\n\n## 产出\n可直接迁移到团队的多代理项目模板。',
   'autogen,multi-agent,llm',
   'https://github.com/microsoft/autogen',
   'attachments/resource-2/autogen-migration-notes.md',
   'liang-zhou', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '34 days', CURRENT_TIMESTAMP - INTERVAL '18 hours'),
  (3, 'CrewAI 文档：角色驱动任务执行框架', 'DOC',
   '## 背景\nCrewAI 强调 Role、Task、Crew 的职责分层。\n\n## 适用场景\n- 快速 Demo\n- 中小规模工作流\n- 需要清晰责任边界的任务编排\n\n## 风险提示\n请在生产环境补齐审计、鉴权与错误恢复。\n\n## 实战建议\n从单 Crew 开始，逐步引入跨 Crew 协作。',
   'crewai,workflow,agent',
   'https://docs.crewai.com/',
   NULL,
   'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '33 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (4, 'Qdrant 向量检索与 Hybrid Search 指南', 'DOC',
   '## 背景\nRAG 质量提升通常从召回开始。\n\n## 方法\n- dense + sparse 混合召回\n- 元数据过滤\n- rerank 二次排序\n\n## 指标\n建议最少追踪 Recall@10、MRR、NDCG。\n\n## 实操\n先打通小语料验证流程，再扩展到多租户场景。',
   'qdrant,vector,hybrid-search,rag',
   'https://qdrant.tech/documentation/',
   'attachments/resource-4/hybrid-search-playbook.pdf',
   'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '32 days', CURRENT_TIMESTAMP - INTERVAL '16 hours'),
  (5, 'LlamaIndex RAG Cookbook', 'DOC',
   '## 背景\nLlamaIndex 在知识库组织与查询引擎方面有大量可复用范式。\n\n## 你会得到\n- ingestion pipeline 模板\n- 评测脚本示例\n- query engine 参数建议\n\n## 复盘建议\n每次参数变更都要保留基准数据与对照实验。',
   'llamaindex,rag,evaluation',
   'https://docs.llamaindex.ai/en/stable/examples/cookbooks/oreilly_course_cookbooks/Module-4/',
   NULL,
   'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '14 hours'),
  (6, 'Playwright 文档：端到端测试最佳实践', 'DOC',
   '## 背景\n前端上线质量很大程度取决于 E2E 覆盖。\n\n## 核心点\n- 稳定选择器策略\n- 合理等待而非盲等\n- 并发执行与重试策略\n- trace 追踪失败路径\n\n## 清单\n上线前至少覆盖登录、核心 CRUD、权限边界。',
   'playwright,e2e,qa,frontend',
   'https://playwright.dev/docs/intro',
   'attachments/resource-6/playwright-checklist.md',
   'yan-lin', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
  (7, 'PostgreSQL 文档：索引与查询规划', 'DOC',
   '## 背景\n慢接口多半是 SQL 计划与索引不匹配。\n\n## 重点\n- 组合索引顺序\n- explain analyze 读法\n- 回表与排序成本\n\n## 实战\n每次上线前固定抽检 TopN 慢查询。',
   'postgres,index,sql,performance',
   'https://www.postgresql.org/docs/current/indexes.html',
   NULL,
   'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '29 days', CURRENT_TIMESTAMP - INTERVAL '20 hours'),
  (8, 'Spring Boot 官方文档：Flyway 数据库迁移', 'DOC',
   '## 背景\n数据库迁移是发布事故高发点。\n\n## 规则\n1. 已上线版本脚本不可改。\n2. 新需求永远新版本号。\n3. repair 只在可控窗口执行。\n\n## 产出\n形成可审计、可回滚、可复盘的迁移流程。',
   'spring-boot,flyway,migration',
   'https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.migration-tool.flyway',
   'attachments/resource-8/flyway-release-runbook.pdf',
   'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
  (9, 'OpenTelemetry 文档：Trace 与 Metrics 关联', 'DOC',
   '## 背景\n定位端到端延迟需要 trace 与 metrics 联动。\n\n## 重点\n- 统一 trace_id 贯穿前后端\n- 关键 span 打点\n- 指标告警阈值设计\n\n## 建议\n将观测数据反哺到发布门禁策略。',
   'opentelemetry,trace,metrics,observability',
   'https://opentelemetry.io/docs/',
   NULL,
   'liang-zhou', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '27 days', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
  (10, 'Redis 官方文档：Streams 与消费者组', 'DOC',
   '## 背景\n异步任务系统常见问题是堆积、丢失与重复消费。\n\n## 重点\n- pending list 监控\n- claim 与重试策略\n- 幂等键设计\n\n## 实战\n结合业务 SLA 设计死信和补偿机制。',
   'redis,stream,queue,reliability',
   'https://redis.io/docs/latest/data-types/streams/',
   'attachments/resource-10/redis-streams-drill.md',
   'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '26 days', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (11, 'RealWorld API 规范实现对照', 'REPO',
   '## 背景\n真实 REST 项目要求契约一致、错误语义清晰。\n\n## 你会学到\n- 资源建模\n- 分页/过滤统一\n- 认证与授权边界\n\n## 建议\n前后端统一对齐字段命名与状态码约定。',
   'api,rest,contract,backend',
   'https://github.com/gothinkster/realworld',
   NULL,
   'qi-song', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (12, '社区技术写作模板（含案例结构）', 'MARKDOWN',
   '## 背景\n高质量技术贴需要“可复现”而非“只说结论”。\n\n## 模板结构\n- 问题背景\n- 复现步骤\n- 指标对比\n- 风险与回滚\n- 行动建议\n\n## 目标\n把经验沉淀为可复用资产。',
   'community,writing,template',
   'https://github.com/openai/openai-cookbook',
   'attachments/resource-12/writing-template.docx',
   'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '24 days', CURRENT_TIMESTAMP - INTERVAL '9 hours');

INSERT INTO resources (
  id, title, type, summary, tags, external_url, object_key, owner_key, visibility, status, created_at, updated_at
)
SELECT
  g,
  format('实战资料 #%s：%s', g,
    (ARRAY[
      'Agent 编排稳定性', 'RAG 召回优化', '发布值班手册', 'Prompt 评测方法', '检索链路排障',
      '前后端契约治理', 'SQL 性能基线', '异步消费可靠性', '观测体系建设', '社区写作模板'
    ])[1 + ((g - 13) % 10)]),
  (ARRAY['DOC', 'REPO', 'MARKDOWN', 'PDF', 'GUIDE'])[1 + ((g - 13) % 5)],
  concat(
    '## 背景', E'\n',
    '该资料来自真实项目实践，包含问题复现、指标对比与回滚策略。', E'\n\n',
    '## 目标', E'\n',
    '帮助团队在上线前完成可执行、可验证、可审计的工程闭环。', E'\n\n',
    '## 关键步骤', E'\n',
    '1. 明确输入输出契约。', E'\n',
    '2. 按环境拆分验证脚本。', E'\n',
    '3. 固化监控与告警阈值。', E'\n',
    '4. 形成故障复盘模板。', E'\n\n',
    '## 经验与坑位', E'\n',
    repeat('上线前只看 happy path 会导致真实流量下问题集中暴露。请至少覆盖边界态、异常态和回滚态。', 5), E'\n\n',
    '## 可操作清单', E'\n',
    '- 验证脚本版本锁定', E'\n',
    '- 关键接口压测记录', E'\n',
    '- 迁移执行窗口与责任人', E'\n',
    '- 失败时一键回滚路径', E'\n\n',
    '## 结论', E'\n',
    '这是一份可直接用于团队培训和上线前检查的大段正文资料。'
  ),
  (ARRAY[
    'agent,orchestration,production', 'rag,retrieval,eval', 'release,runbook,sre',
    'prompt,quality,testing', 'search,debug,performance', 'api,contract,backend',
    'postgres,sql,index', 'redis,stream,retry', 'otel,trace,metrics', 'community,writing,ops'
  ])[1 + ((g - 13) % 10)],
  (ARRAY[
    'https://github.com/langchain-ai/langgraph',
    'https://github.com/microsoft/autogen',
    'https://docs.crewai.com/',
    'https://qdrant.tech/documentation/',
    'https://docs.llamaindex.ai/',
    'https://playwright.dev/docs/intro',
    'https://www.postgresql.org/docs/current/indexes.html',
    'https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.migration-tool.flyway',
    'https://opentelemetry.io/docs/',
    'https://redis.io/docs/latest/data-types/streams/',
    'https://github.com/gothinkster/realworld',
    'https://github.com/openai/openai-cookbook'
  ])[1 + ((g - 13) % 12)],
  CASE
    WHEN g % 3 = 0 THEN format('attachments/batch2/resource-%s/preview.pdf', g)
    WHEN g % 5 = 0 THEN format('attachments/batch2/resource-%s/checklist.md', g)
    ELSE NULL
  END,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
    'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
    'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
  ])[1 + ((g - 13) % 16)],
  CASE WHEN g % 11 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END,
  CASE WHEN g % 13 = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END,
  CURRENT_TIMESTAMP - ((100 - g) || ' hours')::interval,
  CURRENT_TIMESTAMP - ((80 - g) || ' minutes')::interval
FROM generate_series(13, 72) AS g;

INSERT INTO notes (
  id, title, content_md, owner_key, visibility, status, created_at, updated_at
)
VALUES
  (1, 'LangGraph 在线上场景的三层状态设计',
   '# LangGraph 在线上场景的三层状态设计\n\n我们把状态拆成会话态、任务态、审计态三层：\n\n1. 会话态：保留用户上下文与短期记忆。\n2. 任务态：记录工具调用和中间结果。\n3. 审计态：持久化关键决策，便于回溯。\n\n落地建议：checkpoint 只存必要字段，避免状态膨胀。\n\n## 生产经验\n- 大对象走对象存储，状态只存 key。\n- 每次变更都做恢复演练。\n- 关键节点增加审计事件。',
   'liang-zhou', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
  (2, '前端真实接口验收：从 happy path 到边界态',
   '# 前端真实接口验收：从 happy path 到边界态\n\n验收至少覆盖：\n- 401/403 跳转与提示\n- 空列表与分页越界\n- 写入失败重试\n- 刷新后数据回读一致\n\n配套建议：每条关键写路径都要有 E2E 断言。\n\n## 失败复盘模板\n1. 现象\n2. 复现路径\n3. 根因\n4. 修复\n5. 防回归测试',
   'yan-lin', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
  (3, 'RAG 召回调优：Hybrid Search + Rerank 实操',
   '# RAG 召回调优：Hybrid Search + Rerank 实操\n\n先用 sparse+dense 扩召回，再用 cross-encoder 排序。\n\n指标建议：\n- Recall@10\n- MRR\n- 命中片段可解释性评分\n\n## 实操注意\n- 先保证召回覆盖，再追求排序精度。\n- 对失败样本做按主题聚类复盘。',
   'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '4 hours'),
  (4, 'Flyway 迁移冲突处理手册（含 checksum 案例）',
   '# Flyway 迁移冲突处理手册\n\n遇到 checksum mismatch 时，先确认是否改动了已上线版本脚本。\n\n策略：\n1. 已上线版本不改内容。\n2. 新需求走新版本号。\n3. 仅在确认可接受时执行 repair。\n\n## 发布建议\n迁移必须有窗口、预案和回滚动作。',
   'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '17 days', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (5, '发布前性能回归清单（接口到页面）',
   '# 发布前性能回归清单\n\n关键链路：鉴权 -> 聚合接口 -> 页面首屏。\n\n关注：\n- 慢查询与索引命中\n- 前端阻塞请求数\n- 端到端 TTI 与 LCP\n\n## 输出要求\n每次发布都要生成对比报告。',
   'qi-song', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (6, '社区写作：如何把“做了什么”写成“可复现”',
   '# 社区写作：如何把“做了什么”写成“可复现”\n\n帖子结构建议：\n- 背景问题\n- 复现路径\n- 改造方案\n- 指标对比\n- 复盘与风险\n\n结论必须给可执行动作。\n\n## 评分标准\n- 可复现性\n- 数据可信度\n- 结论可操作性',
   'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
  (7, 'Redis Streams 在异步任务里的重试策略草稿',
   '# Redis Streams 在异步任务里的重试策略草稿\n\n待补：消费组故障转移与幂等键设计。\n\n## 当前结论\n先保证重试可控，再追求吞吐极限。',
   'hao-wu', 'PRIVATE', 'DRAFT', CURRENT_TIMESTAMP - INTERVAL '13 days', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
  (8, '本周路线更新计划（内部）',
   '# 本周路线更新计划\n\n待补：路线节点与资料双向引用校验。\n\n## 验收目标\n每条路线至少覆盖 1 个实操案例和 1 个回滚案例。',
   'qi-song', 'PRIVATE', 'DRAFT', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '6 hours');

INSERT INTO notes (
  id, title, content_md, owner_key, visibility, status, created_at, updated_at
)
SELECT
  g,
  format('工程复盘笔记 #%s：%s', g,
    (ARRAY[
      'Agent 链路排障', 'RAG 质量回归', 'Flyway 迁移演练', 'Playwright 失败分析', '接口契约治理',
      '告警降噪策略', '发布回滚演练', '检索性能压测', '写作规范沉淀', 'SRE 值班手册'
    ])[1 + ((g - 9) % 10)]),
  concat(
    '# 复盘背景', E'\n\n',
    '该笔记基于真实项目中的一次上线或故障处理，包含完整时间线与关键决策。', E'\n\n',
    '## 复现路径', E'\n',
    '1. 描述触发条件与输入数据。', E'\n',
    '2. 记录日志、trace 与指标变化。', E'\n',
    '3. 给出最小复现场景。', E'\n\n',
    '## 处理过程', E'\n',
    '- 假设拆分', E'\n',
    '- 证据验证', E'\n',
    '- 修复与回归', E'\n\n',
    '## 结果与风险', E'\n',
    repeat('修复后需持续观察至少一个发布周期，确认无新的性能回退与功能副作用。', 6), E'\n\n',
    '## 行动项', E'\n',
    '- 增加自动化测试', E'\n',
    '- 固化排障脚本', E'\n',
    '- 更新值班文档', E'\n'
  ),
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'ming-xu', 'na-zheng', 'kai-liu',
    'ting-he', 'rui-fan', 'wen-yao', 'bo-jiang', 'xue-an', 'chen-lei', 'su-yu', 'admin-zoe'
  ])[1 + ((g - 9) % 16)],
  CASE WHEN g % 9 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END,
  CASE WHEN g % 14 = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END,
  CURRENT_TIMESTAMP - ((90 - g) || ' hours')::interval,
  CURRENT_TIMESTAMP - ((70 - g) || ' minutes')::interval
FROM generate_series(9, 42) AS g;

INSERT INTO roadmaps (
  id, title, description, owner_key, visibility, status, created_at, updated_at
)
VALUES
  (1, '生产级 Agent 工程落地路线', '从单体 Agent 到多代理协作，覆盖状态管理、工具调用、验收与上线。', 'liang-zhou', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '22 days', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
  (2, 'RAG 检索与评测实战路线', '围绕数据入库、混合检索、重排与评测闭环，构建可持续优化链路。', 'jia-chen', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '21 days', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (3, '前后端真实联调与发布验收路线', '强调契约一致性、E2E 验收、性能回归和发布值班手册。', 'yan-lin', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '5 hours'),
  (4, '社区技术内容建设路线', '帮助作者把资料、路线、笔记串成可追踪的内容资产。', 'qi-song', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '9 hours'),
  (5, '模型路由与成本治理路线', '面向多模型平台的路由、降级、预算控制和稳定性守护。', 'ming-xu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
  (6, '检索系统稳定性路线', '从索引、缓存、降级到观测，覆盖检索系统全链路。', 'rui-fan', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '17 days', CURRENT_TIMESTAMP - INTERVAL '4 hours'),
  (7, '发布应急与回滚演练路线', '聚焦变更窗口、灰度发布、故障演练和复盘模板。', 'kai-liu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (8, '工程写作与知识资产化路线', '把技术实践沉淀为团队可复用资产与培训资料。', 'ting-he', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
  (9, '数据库性能专项路线', '覆盖索引设计、查询计划、迁移安全和容量评估。', 'hao-wu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '5 hours'),
  (10, '前端质量门禁路线', '建立页面性能、可访问性、E2E 与视觉回归门禁。', 'bo-jiang', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '13 days', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
  (11, '观测体系建设路线', '统一日志、指标、追踪和告警策略。', 'chen-lei', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
  (12, '社区治理与运营路线', '覆盖举报处理、内容审核、反馈闭环与精选策略。', 'su-yu', 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '11 days', CURRENT_TIMESTAMP - INTERVAL '1 hour');

INSERT INTO roadmap_nodes (
  id, roadmap_id, parent_id, title, order_no, resource_id, note_id, created_at
)
VALUES
  (1, 1, NULL, '理解 LangGraph 状态图与执行模型', 1, 1, 1, CURRENT_TIMESTAMP - INTERVAL '22 days'),
  (2, 1, NULL, '设计多代理协作与终止条件', 2, 2, NULL, CURRENT_TIMESTAMP - INTERVAL '21 days'),
  (3, 1, NULL, '接入 OpenTelemetry 观测链路', 3, 9, 5, CURRENT_TIMESTAMP - INTERVAL '20 days'),
  (4, 1, NULL, '构建异步任务与重试机制', 4, 10, 7, CURRENT_TIMESTAMP - INTERVAL '19 days'),
  (5, 2, NULL, '搭建向量库与入库流程', 1, 4, NULL, CURRENT_TIMESTAMP - INTERVAL '21 days'),
  (6, 2, NULL, '实现 Hybrid Search 召回', 2, 4, 3, CURRENT_TIMESTAMP - INTERVAL '20 days'),
  (7, 2, NULL, '接入 Rerank 与离线评测', 3, 5, 3, CURRENT_TIMESTAMP - INTERVAL '19 days'),
  (8, 2, NULL, '建立评测看板与迭代节奏', 4, 9, 5, CURRENT_TIMESTAMP - INTERVAL '18 days'),
  (9, 3, NULL, '对齐 API 契约与错误语义', 1, 11, 2, CURRENT_TIMESTAMP - INTERVAL '20 days'),
  (10, 3, NULL, '编排 Playwright 端到端用例', 2, 6, 2, CURRENT_TIMESTAMP - INTERVAL '19 days'),
  (11, 3, NULL, '执行 Flyway 迁移与回归验证', 3, 8, 4, CURRENT_TIMESTAMP - INTERVAL '18 days'),
  (12, 3, NULL, '发布前性能回归与值班手册', 4, 7, 5, CURRENT_TIMESTAMP - INTERVAL '17 days'),
  (13, 4, NULL, '建立选题库与资料池', 1, 12, 6, CURRENT_TIMESTAMP - INTERVAL '19 days'),
  (14, 4, NULL, '发布可复现实战帖', 2, 1, 6, CURRENT_TIMESTAMP - INTERVAL '18 days'),
  (15, 4, NULL, '用互动数据反哺路线更新', 3, 6, NULL, CURRENT_TIMESTAMP - INTERVAL '17 days'),
  (16, 4, NULL, '沉淀季度精选与复盘', 4, 3, 6, CURRENT_TIMESTAMP - INTERVAL '16 days');

INSERT INTO roadmap_nodes (
  id, roadmap_id, parent_id, title, order_no, resource_id, note_id, created_at
)
SELECT
  16 + ((rid - 5) * 6) + seq AS id,
  rid AS roadmap_id,
  CASE
    WHEN seq IN (2, 3) THEN 16 + ((rid - 5) * 6) + 1
    WHEN seq IN (5, 6) THEN 16 + ((rid - 5) * 6) + 4
    ELSE NULL
  END AS parent_id,
  format('阶段 %s - 任务 %s', rid - 4, seq) AS title,
  seq AS order_no,
  CASE
    WHEN seq = 1 THEN (ARRAY[1, 4, 6, 8, 9, 10, 11, 12])[1 + ((rid - 5) % 8)]
    WHEN seq = 2 THEN (ARRAY[2, 4, 6, 9, 13, 15, 18, 21])[1 + ((rid - 5) % 8)]
    WHEN seq = 3 THEN (ARRAY[1, 4, 9, 10, 14, 16, 19, 22])[1 + ((rid - 5) % 8)]
    WHEN seq = 4 THEN (ARRAY[6, 7, 8, 11, 20, 24, 28, 32])[1 + ((rid - 5) % 8)]
    WHEN seq = 5 THEN (ARRAY[4, 6, 9, 12, 30, 36, 42, 48])[1 + ((rid - 5) % 8)]
    ELSE (ARRAY[1, 6, 8, 10, 50, 54, 60, 66])[1 + ((rid - 5) % 8)]
  END AS resource_id,
  CASE
    WHEN seq % 2 = 0 THEN 1 + (((rid * 7) + seq * 3) % 42)
    ELSE NULL
  END AS note_id,
  CURRENT_TIMESTAMP - (((rid - 4) * 8 + seq) || ' hours')::interval
FROM generate_series(5, 12) AS rid
CROSS JOIN generate_series(1, 6) AS seq;

INSERT INTO roadmap_progress (id, roadmap_id, user_key, payload, updated_at)
SELECT
  row_number() OVER (ORDER BY roadmap_id, user_key) AS id,
  roadmap_id,
  user_key,
  jsonb_build_object(
    'completedNodes', completed_nodes,
    'currentNode', current_node,
    'progress', progress
  ) AS payload,
  CURRENT_TIMESTAMP - ((row_number() OVER (ORDER BY roadmap_id, user_key) * 2) || ' hours')::interval
FROM (
  SELECT * FROM (VALUES
    (1, 'yan-lin', ARRAY[1,2], 3, 50),
    (2, 'liang-zhou', ARRAY[5,6], 7, 50),
    (3, 'qi-song', ARRAY[9,10,11], 12, 75),
    (4, 'jia-chen', ARRAY[13,14], 15, 50),
    (5, 'hao-wu', ARRAY[17,18,19], 20, 60),
    (6, 'rui-fan', ARRAY[23,24], 25, 40),
    (7, 'kai-liu', ARRAY[29,30,31], 32, 70),
    (8, 'ting-he', ARRAY[35,36], 37, 45),
    (9, 'wen-yao', ARRAY[41,42], 43, 45),
    (10, 'bo-jiang', ARRAY[47,48,49], 50, 70),
    (11, 'chen-lei', ARRAY[53,54], 55, 50),
    (12, 'su-yu', ARRAY[59,60], 61, 50)
  ) AS p(roadmap_id, user_key, completed_nodes, current_node, progress)
) AS seed_progress;

INSERT INTO comments (id, resource_id, note_id, parent_id, author_key, content, status, created_at)
VALUES
  (1, 1, NULL, NULL, 'yan-lin', 'LangGraph 的 checkpoint 示例很实用，我按它改完后任务恢复稳定很多。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '9 days'),
  (2, 1, NULL, 1, 'liang-zhou', '重点是把大对象排除在 checkpoint 外，状态体积会明显下降。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '20 minutes'),
  (3, 2, NULL, NULL, 'qi-song', 'AutoGen 的多代理对话很强，但要限制回合数，不然成本会飙升。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '8 days'),
  (4, 4, NULL, NULL, 'jia-chen', 'Qdrant 的过滤条件和向量召回结合后，业务准确率提升明显。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '2 hours'),
  (5, 6, NULL, NULL, 'admin-zoe', 'Playwright 的 trace 建议默认打开，发布前查 flaky 很省时间。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '7 days'),
  (6, 8, NULL, NULL, 'hao-wu', 'Flyway 迁移请坚持“已发布版本不改内容”。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '1 hour'),
  (7, NULL, 2, NULL, 'liang-zhou', '这篇把边界态讲得很清楚，特别适合上线前走查。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '4 hours'),
  (8, NULL, 3, NULL, 'yan-lin', '建议补一段 rerank 模型选择标准，方便团队统一口径。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '5 hours'),
  (9, NULL, 4, NULL, 'admin-zoe', '这篇迁移冲突手册已加入值班文档，后续可补 repair 风险边界。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '3 hours'),
  (10, NULL, 6, NULL, 'qi-song', '写作模板很实用，尤其是“可复现步骤”这块。', 'VISIBLE', CURRENT_TIMESTAMP - INTERVAL '5 days');

INSERT INTO comments (id, resource_id, note_id, parent_id, author_key, content, status, created_at)
SELECT
  g,
  13 + ((g - 11) % 60) AS resource_id,
  NULL,
  NULL,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
    'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
    'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
  ])[1 + ((g - 11) % 16)],
  concat(
    '资源线程 #', g, '：已按文档复现关键链路。',
    ' 建议补充基准数据与失败样本，确保结论可重复验证。'
  ),
  'VISIBLE',
  CURRENT_TIMESTAMP - ((260 - g) || ' minutes')::interval
FROM generate_series(11, 210) AS g;

INSERT INTO comments (id, resource_id, note_id, parent_id, author_key, content, status, created_at)
SELECT
  g,
  NULL,
  9 + ((g - 211) % 34) AS note_id,
  NULL,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
    'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
    'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
  ])[1 + ((g - 211) % 16)],
  concat('笔记线程 #', g, '：建议补充压测脚本和截图，便于团队复盘。'),
  'VISIBLE',
  CURRENT_TIMESTAMP - ((500 - g) || ' minutes')::interval
FROM generate_series(211, 260) AS g;

INSERT INTO comments (id, resource_id, note_id, parent_id, author_key, content, status, created_at)
SELECT
  g,
  NULL,
  NULL,
  11 + ((g - 261) % 200) AS parent_id,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
    'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
    'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
  ])[1 + ((g - 261) % 16)],
  concat('回复 #', g, '：已补充复现步骤和关键日志，结论更可靠。'),
  'VISIBLE',
  CURRENT_TIMESTAMP - ((700 - g) || ' minutes')::interval
FROM generate_series(261, 340) AS g;

INSERT INTO favorites (id, resource_id, note_id, user_key, created_at)
VALUES
  (1, 1, NULL, 'yan-lin', CURRENT_TIMESTAMP - INTERVAL '12 days'),
  (2, 2, NULL, 'jia-chen', CURRENT_TIMESTAMP - INTERVAL '11 days'),
  (3, 4, NULL, 'liang-zhou', CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '2 hours'),
  (4, 6, NULL, 'qi-song', CURRENT_TIMESTAMP - INTERVAL '10 days'),
  (5, 8, NULL, 'yan-lin', CURRENT_TIMESTAMP - INTERVAL '9 days'),
  (6, NULL, 2, 'qi-song', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '1 hour'),
  (7, NULL, 3, 'hao-wu', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '3 hours'),
  (8, NULL, 5, 'jia-chen', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '4 hours');

INSERT INTO favorites (id, resource_id, note_id, user_key, created_at)
SELECT
  g,
  CASE WHEN g % 2 = 0 THEN 1 + ((g - 9) % 72) ELSE NULL END,
  CASE WHEN g % 2 = 1 THEN 1 + ((g - 9) % 42) ELSE NULL END,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'ming-xu', 'na-zheng', 'kai-liu',
    'ting-he', 'rui-fan', 'wen-yao', 'bo-jiang', 'xue-an', 'chen-lei', 'su-yu', 'admin-zoe'
  ])[1 + ((g - 9) % 16)],
  CURRENT_TIMESTAMP - ((800 - g) || ' minutes')::interval
FROM generate_series(9, 120) AS g;

INSERT INTO likes (id, resource_id, note_id, user_key, created_at)
VALUES
  (1, 1, NULL, 'jia-chen', CURRENT_TIMESTAMP - INTERVAL '12 days'),
  (2, 1, NULL, 'qi-song', CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '1 hour'),
  (3, 2, NULL, 'yan-lin', CURRENT_TIMESTAMP - INTERVAL '11 days'),
  (4, 4, NULL, 'liang-zhou', CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '5 hours'),
  (5, 6, NULL, 'admin-zoe', CURRENT_TIMESTAMP - INTERVAL '10 days'),
  (6, 9, NULL, 'hao-wu', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '2 hours'),
  (7, 11, NULL, 'yan-lin', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '6 hours'),
  (8, NULL, 1, 'yan-lin', CURRENT_TIMESTAMP - INTERVAL '10 days'),
  (9, NULL, 2, 'liang-zhou', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '3 hours'),
  (10, NULL, 3, 'qi-song', CURRENT_TIMESTAMP - INTERVAL '9 days'),
  (11, NULL, 4, 'jia-chen', CURRENT_TIMESTAMP - INTERVAL '8 days'),
  (12, NULL, 6, 'admin-zoe', CURRENT_TIMESTAMP - INTERVAL '7 days');

INSERT INTO likes (id, resource_id, note_id, user_key, created_at)
SELECT
  g,
  CASE WHEN g % 3 <> 0 THEN 1 + ((g - 13) % 72) ELSE NULL END,
  CASE WHEN g % 3 = 0 THEN 1 + ((g - 13) % 42) ELSE NULL END,
  (ARRAY[
    'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'ming-xu', 'na-zheng', 'kai-liu',
    'ting-he', 'rui-fan', 'wen-yao', 'bo-jiang', 'xue-an', 'chen-lei', 'su-yu', 'admin-zoe'
  ])[1 + ((g - 13) % 16)],
  CURRENT_TIMESTAMP - ((1200 - g) || ' minutes')::interval
FROM generate_series(13, 220) AS g;

INSERT INTO reports (id, target_type, target_id, reporter_key, reason, details, status, created_at, resolved_at, resolved_by)
VALUES
  (1, 'NOTE', 7, 'admin-zoe', '草稿信息不完整', '该草稿仅有提纲，建议作者补充重试与幂等示例后再公开。', 'OPEN', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, NULL),
  (2, 'RESOURCE', 12, 'admin-zoe', '外链需补充来源说明', '已要求作者补充引用范围和适用边界说明，防止误用。', 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '4 days', 'admin-zoe'),
  (3, 'RESOURCE', 36, 'su-yu', '附件命名不规范', '附件名称缺少版本号，已通知作者更新。', 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day', 'admin-zoe'),
  (4, 'NOTE', 28, 'wen-yao', '内容需要补充证据', '建议增加日志片段与指标截图证明。', 'OPEN', CURRENT_TIMESTAMP - INTERVAL '20 hours', NULL, NULL);

INSERT INTO admin_audit_logs (id, action, target_type, target_id, operator_key, details, created_at)
VALUES
  (1, 'Resolve Report', 'REPORT', '2', 'admin-zoe', '{"summary":"已补充来源说明并通过复核"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '4 days'),
  (2, 'Queue Review', 'NOTE', '7', 'admin-zoe', '{"summary":"保持草稿状态，等待作者补全文"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '3 days'),
  (3, 'Roadmap Publish', 'ROADMAP', '3', 'admin-zoe', '{"summary":"联调验收路线已通过发布检查"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (4, 'Resolve Report', 'REPORT', '3', 'admin-zoe', '{"summary":"附件命名规范已修复"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (5, 'Escalate Review', 'NOTE', '28', 'admin-zoe', '{"summary":"要求作者补齐证据再公开"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '10 hours');

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('resources', 'id'), COALESCE((SELECT MAX(id) FROM resources), 1), true);
SELECT setval(pg_get_serial_sequence('notes', 'id'), COALESCE((SELECT MAX(id) FROM notes), 1), true);
SELECT setval(pg_get_serial_sequence('roadmaps', 'id'), COALESCE((SELECT MAX(id) FROM roadmaps), 1), true);
SELECT setval(pg_get_serial_sequence('roadmap_nodes', 'id'), COALESCE((SELECT MAX(id) FROM roadmap_nodes), 1), true);
SELECT setval(pg_get_serial_sequence('comments', 'id'), COALESCE((SELECT MAX(id) FROM comments), 1), true);
SELECT setval(pg_get_serial_sequence('favorites', 'id'), COALESCE((SELECT MAX(id) FROM favorites), 1), true);
SELECT setval(pg_get_serial_sequence('likes', 'id'), COALESCE((SELECT MAX(id) FROM likes), 1), true);
SELECT setval(pg_get_serial_sequence('reports', 'id'), COALESCE((SELECT MAX(id) FROM reports), 1), true);
SELECT setval(pg_get_serial_sequence('admin_audit_logs', 'id'), COALESCE((SELECT MAX(id) FROM admin_audit_logs), 1), true);
SELECT setval(pg_get_serial_sequence('roadmap_progress', 'id'), COALESCE((SELECT MAX(id) FROM roadmap_progress), 1), true);

COMMIT;
