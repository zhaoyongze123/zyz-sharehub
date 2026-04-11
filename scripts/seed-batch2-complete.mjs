#!/usr/bin/env node

const BASE_URL = process.env.SHAREHUB_BASE_URL || 'http://127.0.0.1:8080/api'
const CONCURRENCY = Number(process.env.SEED_CONCURRENCY || 12)

const users = [
  'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
  'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
  'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
]

async function api(path, { method = 'GET', userKey, body } = {}) {
  const headers = {}
  if (userKey) headers['X-User-Key'] = userKey
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  })
  const text = await res.text()
  let payload
  try {
    payload = JSON.parse(text)
  } catch {
    throw new Error(`非 JSON 响应: ${path}, status=${res.status}, body=${text.slice(0, 160)}`)
  }
  if (!res.ok || !payload.success) {
    throw new Error(`接口失败: ${path}, status=${res.status}, body=${JSON.stringify(payload).slice(0, 240)}`)
  }
  return payload.data
}

function noteContent(index) {
  return [
    '# 二期复盘笔记',
    `这是一篇用于二期扩容的真实结构化笔记 #${index}。`,
    '',
    '## 复现路径',
    '1. 记录输入、环境和触发条件。',
    '2. 保留日志、trace 与指标截图。',
    '3. 给出最小复现命令。',
    '',
    '## 处理过程',
    '- 根因定位',
    '- 方案对比',
    '- 修复回归',
    '',
    '## 结果与风险',
    '上线后需持续观察一个发布周期，确保无新的性能回退。'.repeat(5),
    '',
    '## 行动项',
    '- 补充自动化测试',
    '- 更新值班文档',
    '- 同步风险边界'
  ].join('\n')
}

async function withRetry(task, retries = 3) {
  let lastError
  for (let i = 0; i < retries; i += 1) {
    try {
      return await task()
    } catch (error) {
      lastError = error
      if (i === retries - 1) {
        throw lastError
      }
    }
  }
  throw lastError
}

async function runWithConcurrency(items, worker) {
  const queue = [...items]
  const results = []
  const runners = Array.from({ length: Math.min(CONCURRENCY, items.length) }, async () => {
    while (queue.length > 0) {
      const item = queue.shift()
      const result = await worker(item)
      results.push(result)
    }
  })
  await Promise.all(runners)
  return results
}

async function main() {
  const resourcesPage = await api('/resources?page=0&pageSize=200')
  const resourceIds = resourcesPage.items.map((item) => item.id)
  if (resourceIds.length < 20) {
    throw new Error(`资源不足，当前仅 ${resourceIds.length} 条，无法构造 200+ 评论线程`) 
  }

  const notesToCreate = Array.from({ length: 35 }, (_, i) => i + 1)
  const createdNotes = await runWithConcurrency(notesToCreate, async (index) => {
    const userKey = users[index % users.length]
    return withRetry(() => api('/notes', {
      method: 'POST',
      userKey,
      body: {
        title: `第二批工程复盘笔记 #${index}`,
        contentMd: noteContent(index),
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    }))
  })

  const sharedResources = resourceIds.slice(0, 12)
  const sharedNotes = createdNotes.map((n) => n.id).slice(0, 12)

  const roadmapSeed = [
    '第二批学习图谱：Agent 工程化',
    '第二批学习图谱：检索与评测',
    '第二批学习图谱：发布与值班',
    '第二批学习图谱：质量与治理',
    '第二批学习图谱：可观测性专项',
    '第二批学习图谱：数据库与性能'
  ]

  const createdRoadmaps = []
  for (let i = 0; i < roadmapSeed.length; i += 1) {
    const userKey = users[i % users.length]
    const roadmap = await api('/roadmaps', {
      method: 'POST',
      userKey,
      body: {
        title: roadmapSeed[i],
        description: '验证同一资料被多路线复用的图谱复杂度。',
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    })
    createdRoadmaps.push(roadmap)

    for (let n = 0; n < 9; n += 1) {
      const resourceId = sharedResources[(i * 2 + n) % sharedResources.length]
      const noteId = sharedNotes[(i * 3 + n) % sharedNotes.length]
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

  const commentTargets = resourceIds.slice(0, 25)
  const topLevelSeed = Array.from({ length: 220 }, (_, i) => i + 1)
  const topLevelComments = await runWithConcurrency(topLevelSeed, async (index) => {
    const userKey = users[index % users.length]
    const resourceId = commentTargets[index % commentTargets.length]
    return withRetry(() => api(`/resources/${resourceId}/comments`, {
      method: 'POST',
      userKey,
      body: {
        content: `第二批评论线程 #${index}：已按资料执行复现，建议补充失败样本与对照数据。`
      }
    }))
  })

  const replySeed = Array.from({ length: 90 }, (_, i) => i + 1)
  await runWithConcurrency(replySeed, async (index) => {
    const userKey = users[(index + 5) % users.length]
    const parentId = topLevelComments[index % topLevelComments.length].id
    return withRetry(() => api(`/comments/${parentId}/reply`, {
      method: 'POST',
      userKey,
      body: {
        content: `第二批回复 #${index}：已补齐日志、截图与回滚记录。`
      }
    }))
  })

  const resourceAfter = await api('/resources?page=0&pageSize=1')
  const roadmapAfter = await api('/roadmaps?page=1&pageSize=200')

  console.log(JSON.stringify({
    success: true,
    created: {
      notes: createdNotes.length,
      roadmaps: createdRoadmaps.length,
      comments: topLevelComments.length,
      replies: replySeed.length
    },
    totals: {
      resourcesPublished: resourceAfter.total,
      roadmapsPublished: roadmapAfter.total
    },
    sampleRoadmapIds: createdRoadmaps.map((item) => item.id),
    sampleNoteIds: createdNotes.slice(0, 8).map((item) => item.id)
  }, null, 2))
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
