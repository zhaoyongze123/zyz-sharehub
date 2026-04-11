#!/usr/bin/env node

const BASE_URL = process.env.SHAREHUB_BASE_URL || 'http://127.0.0.1:8080/api'
const CONCURRENCY = Number(process.env.SEED_CONCURRENCY || 8)

const users = [
  'liang-zhou', 'yan-lin', 'hao-wu', 'jia-chen', 'qi-song', 'admin-zoe',
  'ming-xu', 'na-zheng', 'kai-liu', 'ting-he', 'rui-fan', 'wen-yao',
  'bo-jiang', 'xue-an', 'chen-lei', 'su-yu'
]

function buildLongNote(index) {
  return [
    '# 二期实战笔记',
    `笔记 #${index}：真实问题、真实证据、真实复盘。`,
    '',
    '## 复现步骤',
    '1. 固定输入样本与版本。',
    '2. 采集日志、trace、指标。',
    '3. 记录回滚路径与恢复时间。',
    '',
    '## 关键结论',
    '只做 happy path 的测试在生产中风险极高。'.repeat(8),
    '',
    '## 行动项',
    '- 增加自动化回归',
    '- 固化排障脚本',
    '- 更新值班手册'
  ].join('\n')
}

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
    throw new Error(`非 JSON 响应: ${path}, status=${res.status}, body=${text.slice(0, 120)}`)
  }
  if (!res.ok || !payload.success) {
    throw new Error(`接口失败: ${path}, status=${res.status}, body=${JSON.stringify(payload).slice(0, 240)}`)
  }
  return payload.data
}

async function runWithConcurrency(items, worker) {
  const queue = [...items]
  const runners = Array.from({ length: Math.min(CONCURRENCY, items.length) }, async () => {
    while (queue.length) {
      const item = queue.shift()
      await worker(item)
    }
  })
  await Promise.all(runners)
}

function countCommentTree(nodes) {
  let count = 0
  for (const node of nodes) {
    count += 1
    if (Array.isArray(node.children) && node.children.length) {
      count += countCommentTree(node.children)
    }
  }
  return count
}

async function main() {
  const resources = await api('/resources?page=0&pageSize=200')
  const resourceIds = resources.items.map((item) => item.id)

  const noteTotals = await Promise.all(
    users.map((user) => api('/notes?page=1&pageSize=1', { userKey: user }).then((data) => data.total).catch(() => 0))
  )
  const notesBefore = noteTotals.reduce((sum, value) => sum + value, 0)
  const notesNeed = Math.max(0, 32 - notesBefore)

  if (notesNeed > 0) {
    await runWithConcurrency(Array.from({ length: notesNeed }, (_, i) => i + 1), async (i) => {
      const userKey = users[i % users.length]
      await api('/notes', {
        method: 'POST',
        userKey,
        body: {
          title: `第二批工程复盘笔记-补齐 #${i}`,
          contentMd: buildLongNote(i),
          visibility: 'PUBLIC',
          status: 'PUBLISHED'
        }
      })
    })
  }

  const roadmaps = await api('/roadmaps?page=1&pageSize=200')
  const roadmapNeed = Math.max(0, 10 - roadmaps.total)
  const sharedResourcePool = resourceIds.slice(0, 10)

  for (let i = 0; i < roadmapNeed; i += 1) {
    const userKey = users[i % users.length]
    const roadmap = await api('/roadmaps', {
      method: 'POST',
      userKey,
      body: {
        title: `第二批复用路线 #${i + 1}`,
        description: '用于验证同一资料被多路线复用。',
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    })

    for (let n = 0; n < 8; n += 1) {
      const resourceId = sharedResourcePool[(i + n) % sharedResourcePool.length]
      await api(`/roadmaps/${roadmap.id}/nodes`, {
        method: 'POST',
        userKey,
        body: {
          parentId: null,
          title: `阶段 ${n + 1}：复用资源 ${resourceId}`,
          orderNo: n + 1,
          resourceId,
          noteId: null
        }
      })
    }
  }

  let commentsBefore = 0
  for (const id of resourceIds.slice(0, 80)) {
    const tree = await api(`/resources/${id}/comments`)
    commentsBefore += countCommentTree(tree)
  }

  const commentNeed = Math.max(0, 220 - commentsBefore)
  const targetResources = resourceIds.slice(0, 20)
  const createdCommentIds = []

  if (commentNeed > 0) {
    await runWithConcurrency(Array.from({ length: commentNeed }, (_, i) => i + 1), async (i) => {
      const userKey = users[i % users.length]
      const resourceId = targetResources[i % targetResources.length]
      const comment = await api(`/resources/${resourceId}/comments`, {
        method: 'POST',
        userKey,
        body: {
          content: `第二批评论线程补齐 #${i}：复现步骤已补充，包含关键日志与回滚结论。`
        }
      })
      createdCommentIds.push(comment.id)
    })

    const repliesNeed = Math.max(30, Math.floor(commentNeed * 0.3))
    await runWithConcurrency(Array.from({ length: repliesNeed }, (_, i) => i + 1), async (i) => {
      const parentId = createdCommentIds[i % createdCommentIds.length]
      const userKey = users[(i + 4) % users.length]
      await api(`/comments/${parentId}/reply`, {
        method: 'POST',
        userKey,
        body: {
          content: `补充回复 #${i}：已增加对照数据，结论可复核。`
        }
      })
    })
  }

  const noteTotalsAfter = await Promise.all(
    users.map((user) => api('/notes?page=1&pageSize=1', { userKey: user }).then((data) => data.total).catch(() => 0))
  )
  const notesAfter = noteTotalsAfter.reduce((sum, value) => sum + value, 0)

  const roadmapsAfter = await api('/roadmaps?page=1&pageSize=200')

  let commentsAfter = 0
  for (const id of resourceIds.slice(0, 80)) {
    const tree = await api(`/resources/${id}/comments`)
    commentsAfter += countCommentTree(tree)
  }

  console.log(JSON.stringify({
    success: true,
    totals: {
      resourcesPublished: resources.total,
      notesBefore,
      notesAfter,
      roadmapsAfter: roadmapsAfter.total,
      commentsBefore,
      commentsAfter
    },
    increments: {
      notesCreated: notesNeed,
      roadmapsCreated: roadmapNeed,
      commentThreadsCreated: commentNeed
    }
  }, null, 2))
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
