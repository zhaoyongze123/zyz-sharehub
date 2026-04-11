#!/usr/bin/env node

const BASE_URL = process.env.SHAREHUB_BASE_URL || 'http://127.0.0.1:8080/api'

const users = [
  'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
  'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
  'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
]

const sourceUrls = [
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
]

const resourceTopics = [
  'Agent 编排稳定性', 'RAG 召回优化', '发布值班手册', 'Prompt 评测方法', '检索链路排障',
  '前后端契约治理', 'SQL 性能基线', '异步消费可靠性', '观测体系建设', '社区写作模板'
]

function longSummary(index) {
  return [
    '## 背景',
    `该资料 #${index} 基于真实项目实践，包含问题复现、指标对比与回滚策略。`,
    '',
    '## 目标',
    '帮助团队在上线前完成可执行、可验证、可审计的工程闭环。',
    '',
    '## 关键步骤',
    '1. 明确输入输出契约。',
    '2. 按环境拆分验证脚本。',
    '3. 固化监控与告警阈值。',
    '4. 形成故障复盘模板。',
    '',
    '## 经验与坑位',
    '上线前只看 happy path 会导致真实流量下问题集中暴露。请至少覆盖边界态、异常态和回滚态。'.repeat(6),
    '',
    '## 可操作清单',
    '- 验证脚本版本锁定',
    '- 关键接口压测记录',
    '- 迁移执行窗口与责任人',
    '- 失败时一键回滚路径',
    '',
    '## 结论',
    '这是一份可直接用于团队培训和上线前检查的大段正文资料。'
  ].join('\n')
}

function longNoteContent(index) {
  return [
    '# 复盘背景',
    `该笔记 #${index} 基于真实项目中的一次上线或故障处理，包含完整时间线与关键决策。`,
    '',
    '## 复现路径',
    '1. 描述触发条件与输入数据。',
    '2. 记录日志、trace 与指标变化。',
    '3. 给出最小复现场景。',
    '',
    '## 处理过程',
    '- 假设拆分',
    '- 证据验证',
    '- 修复与回归',
    '',
    '## 结果与风险',
    '修复后需持续观察至少一个发布周期，确认无新的性能回退与功能副作用。'.repeat(6),
    '',
    '## 行动项',
    '- 增加自动化测试',
    '- 固化排障脚本',
    '- 更新值班文档'
  ].join('\n')
}

async function api(path, { method = 'GET', userKey, body } = {}) {
  const headers = {}
  if (userKey) {
    headers['X-User-Key'] = userKey
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  })

  const text = await res.text()
  let data
  try {
    data = JSON.parse(text)
  } catch {
    throw new Error(`接口返回非 JSON: ${path}, status=${res.status}, body=${text.slice(0, 200)}`)
  }

  if (!res.ok || !data.success) {
    throw new Error(`接口调用失败: ${path}, status=${res.status}, body=${JSON.stringify(data).slice(0, 300)}`)
  }
  return data.data
}

async function main() {
  const createdResourceIds = []
  const createdNoteIds = []
  const createdRoadmapIds = []
  const createdCommentIds = []

  const resourceTotalBefore = await api('/resources?page=0&pageSize=1')

  for (let i = 0; i < 45; i += 1) {
    const userKey = users[i % users.length]
    const title = `第二批实战资料 #${i + 1}：${resourceTopics[i % resourceTopics.length]}`
    const resource = await api('/resources', {
      method: 'POST',
      userKey,
      body: {
        title,
        type: i % 2 === 0 ? 'DOC' : 'REPO',
        category: i % 2 === 0 ? 'DOC' : 'REPO',
        summary: longSummary(i + 1),
        tags: ['batch2', 'real', 'production', resourceTopics[i % resourceTopics.length].replace(/\s+/g, '-')],
        visibility: 'PUBLIC',
        status: 'PUBLISHED',
        externalUrl: sourceUrls[i % sourceUrls.length],
        objectKey: i % 3 === 0 ? `attachments/batch2/resource-${i + 1}/preview.${i % 2 === 0 ? 'pdf' : 'md'}` : null
      }
    })

    await api(`/resources/${resource.id}/publish`, { method: 'POST', userKey })
    createdResourceIds.push(resource.id)
  }

  for (let i = 0; i < 32; i += 1) {
    const userKey = users[i % users.length]
    const note = await api('/notes', {
      method: 'POST',
      userKey,
      body: {
        title: `第二批工程复盘笔记 #${i + 1}`,
        contentMd: longNoteContent(i + 1),
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    })
    createdNoteIds.push(note.id)
  }

  const roadmapTitles = [
    '第二批学习图谱：Agent 工程化',
    '第二批学习图谱：检索与评测',
    '第二批学习图谱：发布与值班',
    '第二批学习图谱：质量与治理'
  ]

  const sharedResourcePool = createdResourceIds.slice(0, 12)
  const sharedNotePool = createdNoteIds.slice(0, 12)

  for (let i = 0; i < roadmapTitles.length; i += 1) {
    const userKey = users[i % users.length]
    const roadmap = await api('/roadmaps', {
      method: 'POST',
      userKey,
      body: {
        title: roadmapTitles[i],
        description: '用于验证同一资料被多路线复用的复杂学习图谱。',
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    })

    createdRoadmapIds.push(roadmap.id)

    for (let n = 0; n < 8; n += 1) {
      const resourceId = sharedResourcePool[(i * 3 + n) % sharedResourcePool.length]
      const noteId = sharedNotePool[(i * 5 + n) % sharedNotePool.length]
      await api(`/roadmaps/${roadmap.id}/nodes`, {
        method: 'POST',
        userKey,
        body: {
          parentId: null,
          title: `阶段 ${n + 1}：复用资料 ${resourceId}`,
          orderNo: n + 1,
          resourceId,
          noteId: n % 2 === 0 ? noteId : null
        }
      })
    }
  }

  const commentTargets = createdResourceIds.slice(0, 20)

  for (let i = 0; i < 210; i += 1) {
    const userKey = users[i % users.length]
    const resourceId = commentTargets[i % commentTargets.length]
    const comment = await api(`/resources/${resourceId}/comments`, {
      method: 'POST',
      userKey,
      body: {
        content: `第二批评论线程 #${i + 1}：已按文档复现关键链路，建议补充基准数据与失败样本。`
      }
    })
    createdCommentIds.push(comment.id)
  }

  for (let i = 0; i < 80; i += 1) {
    const userKey = users[(i + 3) % users.length]
    const parentCommentId = createdCommentIds[i % createdCommentIds.length]
    await api(`/comments/${parentCommentId}/reply`, {
      method: 'POST',
      userKey,
      body: {
        content: `第二批回复 #${i + 1}：已补充日志片段和截图，结论更可靠。`
      }
    })
  }

  const resourceTotalAfter = await api('/resources?page=0&pageSize=1')

  console.log(JSON.stringify({
    success: true,
    created: {
      resources: createdResourceIds.length,
      notes: createdNoteIds.length,
      roadmaps: createdRoadmapIds.length,
      comments: createdCommentIds.length,
      replies: 80
    },
    totals: {
      resourcePublishedBefore: resourceTotalBefore.total,
      resourcePublishedAfter: resourceTotalAfter.total
    },
    samples: {
      resourceIds: createdResourceIds.slice(0, 8),
      noteIds: createdNoteIds.slice(0, 8),
      roadmapIds: createdRoadmapIds
    }
  }, null, 2))
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
