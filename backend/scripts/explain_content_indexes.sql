BEGIN;

SET LOCAL client_min_messages = WARNING;

INSERT INTO resources (
  title,
  type,
  summary,
  owner_key,
  visibility,
  status,
  created_at,
  updated_at
)
SELECT
  'EXPLAIN 资源 ' || g,
  'PDF',
  'bulk explain seed',
  CASE WHEN g <= 1200 THEN 'explain-owner' ELSE 'other-owner' END,
  CASE WHEN g % 2 = 0 THEN 'PUBLIC' ELSE 'PRIVATE' END,
  CASE WHEN g % 3 = 0 THEN 'PUBLISHED' ELSE 'DRAFT' END,
  now() - make_interval(days => (g % 30)),
  now() - make_interval(mins => g)
FROM generate_series(1, 2400) AS g;

INSERT INTO notes (
  title,
  content_md,
  owner_key,
  visibility,
  status,
  category,
  is_official,
  is_pinned,
  created_at,
  updated_at
)
SELECT
  'EXPLAIN 笔记 ' || g,
  '# bulk explain seed ' || g,
  CASE WHEN g <= 2000 THEN 'explain-owner' ELSE 'other-owner' END,
  CASE WHEN g % 2 = 0 THEN 'PUBLIC' ELSE 'PRIVATE' END,
  CASE WHEN g % 3 = 0 THEN 'PUBLISHED' ELSE 'DRAFT' END,
  'backend',
  FALSE,
  (g % 40 = 0),
  now() - make_interval(days => (g % 30)),
  now() - make_interval(mins => g)
FROM generate_series(1, 4000) AS g;

INSERT INTO roadmaps (
  title,
  description,
  owner_key,
  visibility,
  status,
  created_at,
  updated_at
)
SELECT
  'EXPLAIN 路线 ' || g,
  'bulk explain seed',
  CASE WHEN g <= 1200 THEN 'explain-owner' ELSE 'other-owner' END,
  'PUBLIC',
  CASE WHEN g % 2 = 0 THEN 'PUBLISHED' ELSE 'DRAFT' END,
  now() - make_interval(days => (g % 30)),
  now() - make_interval(mins => g)
FROM generate_series(1, 2400) AS g;

INSERT INTO roadmap_nodes (
  roadmap_id,
  title,
  order_no,
  created_at
)
SELECT
  id,
  'EXPLAIN 节点 ' || id,
  1,
  now()
FROM roadmaps
WHERE title LIKE 'EXPLAIN 路线 %';

INSERT INTO roadmap_enrollments (
  roadmap_id,
  user_key,
  status,
  started_at,
  completed_at,
  created_at,
  updated_at
)
SELECT
  id,
  'explain-user',
  CASE
    WHEN id % 4 = 0 THEN 'ACTIVE'
    WHEN id % 4 = 1 THEN 'PAUSED'
    WHEN id % 4 = 2 THEN 'COMPLETED'
    ELSE 'QUIT'
  END,
  now() - interval '5 day',
  CASE WHEN id % 4 = 2 THEN now() - interval '1 day' ELSE NULL END,
  now() - interval '5 day',
  now() - make_interval(mins => ((id % 120)::int))
FROM roadmaps
WHERE title LIKE 'EXPLAIN 路线 %'
LIMIT 1200;

INSERT INTO roadmap_node_progress (
  roadmap_id,
  node_id,
  user_key,
  status,
  completed_at,
  created_at,
  updated_at
)
SELECT
  n.roadmap_id,
  n.id,
  'explain-user',
  CASE
    WHEN n.id % 4 = 0 THEN 'COMPLETED'
    WHEN n.id % 4 = 1 THEN 'IN_PROGRESS'
    WHEN n.id % 4 = 2 THEN 'SKIPPED'
    ELSE 'NOT_STARTED'
  END,
  CASE WHEN n.id % 4 = 0 THEN now() - interval '1 day' ELSE NULL END,
  now() - interval '3 day',
  now() - make_interval(mins => ((n.id % 120)::int))
FROM roadmap_nodes n
JOIN roadmaps r ON r.id = n.roadmap_id
WHERE r.title LIKE 'EXPLAIN 路线 %'
LIMIT 1200;

INSERT INTO reports (
  target_type,
  target_id,
  reporter_key,
  reason,
  status,
  resolved_at,
  resolved_by,
  created_at
)
SELECT
  CASE WHEN g % 2 = 0 THEN 'RESOURCE' ELSE 'NOTE' END,
  g,
  'explain-reporter',
  'bulk explain seed',
  CASE WHEN g % 3 = 0 THEN 'RESOLVED' ELSE 'OPEN' END,
  CASE WHEN g % 3 = 0 THEN now() - interval '1 day' ELSE NULL END,
  CASE WHEN g % 3 = 0 THEN 'admin' ELSE NULL END,
  now() - make_interval(mins => g)
FROM generate_series(1, 3000) AS g;

ANALYZE resources;
ANALYZE notes;
ANALYZE roadmaps;
ANALYZE roadmap_nodes;
ANALYZE roadmap_enrollments;
ANALYZE roadmap_node_progress;
ANALYZE reports;

\echo '--- resources owner/status/visibility 列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM resources
WHERE owner_key = 'explain-owner'
  AND status = 'PUBLISHED'
  AND visibility = 'PUBLIC'
  AND deleted_at IS NULL
ORDER BY updated_at DESC
LIMIT 12;

\echo '--- resources 广场列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM resources
WHERE status IN ('PUBLISHED')
  AND visibility = 'PUBLIC'
  AND deleted_at IS NULL
ORDER BY updated_at DESC
LIMIT 12;

\echo '--- notes owner/status 列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM notes
WHERE owner_key = 'explain-owner'
  AND status = 'PUBLISHED'
  AND deleted_at IS NULL
ORDER BY id DESC
LIMIT 10;

\echo '--- notes 笔记广场列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM notes
WHERE visibility = 'PUBLIC'
  AND status = 'PUBLISHED'
  AND deleted_at IS NULL
ORDER BY is_pinned DESC, id DESC
LIMIT 10;

\echo '--- roadmaps owner/status 列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM roadmaps
WHERE owner_key = 'explain-owner'
  AND status = 'PUBLISHED'
  AND deleted_at IS NULL
ORDER BY id DESC
LIMIT 10;

\echo '--- roadmap_enrollments 我的路线'
EXPLAIN (ANALYZE, BUFFERS)
SELECT roadmap_id
FROM roadmap_enrollments
WHERE user_key = 'explain-user'
  AND status = 'ACTIVE'
ORDER BY updated_at DESC
LIMIT 10;

\echo '--- roadmap_node_progress 用户路线节点统计'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*)
FROM roadmap_node_progress
WHERE user_key = 'explain-user'
  AND roadmap_id = (
    SELECT id
    FROM roadmaps
    WHERE owner_key = 'explain-owner'
      AND status = 'PUBLISHED'
    ORDER BY id DESC
    LIMIT 1
  )
  AND status = 'COMPLETED';

\echo '--- reports 举报审核列表'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id
FROM reports
WHERE status = 'OPEN'
  AND target_type = 'RESOURCE'
ORDER BY id DESC
LIMIT 20;

ROLLBACK;
