-- 任务 6 用户关联修复脚本
-- 适用于 users 表缺失历史 login，导致 user_id 无法回填或出现孤儿数据的场景。

WITH referenced_logins AS (
  SELECT owner_key AS login
  FROM resources
  WHERE owner_key IS NOT NULL AND owner_key <> ''
  UNION
  SELECT owner_key AS login
  FROM roadmaps
  WHERE owner_key IS NOT NULL AND owner_key <> ''
  UNION
  SELECT owner_key AS login
  FROM notes
  WHERE owner_key IS NOT NULL AND owner_key <> ''
  UNION
  SELECT user_key AS login
  FROM roadmap_enrollments
  WHERE user_key IS NOT NULL AND user_key <> ''
  UNION
  SELECT user_key AS login
  FROM roadmap_progress
  WHERE user_key IS NOT NULL AND user_key <> ''
  UNION
  SELECT user_key AS login
  FROM roadmap_node_progress
  WHERE user_key IS NOT NULL AND user_key <> ''
  UNION
  SELECT author_key AS login
  FROM comments
  WHERE author_key IS NOT NULL AND author_key <> ''
  UNION
  SELECT user_key AS login
  FROM favorites
  WHERE user_key IS NOT NULL AND user_key <> ''
  UNION
  SELECT user_key AS login
  FROM likes
  WHERE user_key IS NOT NULL AND user_key <> ''
  UNION
  SELECT reporter_key AS login
  FROM reports
  WHERE reporter_key IS NOT NULL AND reporter_key <> ''
)
INSERT INTO users (login, name, status, created_at, updated_at)
SELECT rl.login, rl.login, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM referenced_logins rl
LEFT JOIN users u ON u.login = rl.login
WHERE u.id IS NULL;

UPDATE resources r
SET user_id = u.id
FROM users u
WHERE r.owner_key = u.login
  AND (r.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = r.user_id));

UPDATE roadmaps r
SET user_id = u.id
FROM users u
WHERE r.owner_key = u.login
  AND (r.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = r.user_id));

UPDATE notes n
SET user_id = u.id
FROM users u
WHERE n.owner_key = u.login
  AND (n.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = n.user_id));

UPDATE roadmap_enrollments e
SET user_id = u.id
FROM users u
WHERE e.user_key = u.login
  AND (e.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = e.user_id));

UPDATE roadmap_progress p
SET user_id = u.id
FROM users u
WHERE p.user_key = u.login
  AND (p.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = p.user_id));

UPDATE roadmap_node_progress p
SET user_id = u.id
FROM users u
WHERE p.user_key = u.login
  AND (p.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = p.user_id));

UPDATE comments c
SET user_id = u.id
FROM users u
WHERE c.author_key = u.login
  AND (c.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = c.user_id));

UPDATE favorites f
SET user_id = u.id
FROM users u
WHERE f.user_key = u.login
  AND (f.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = f.user_id));

UPDATE likes l
SET user_id = u.id
FROM users u
WHERE l.user_key = u.login
  AND (l.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = l.user_id));

UPDATE reports r
SET user_id = u.id
FROM users u
WHERE r.reporter_key = u.login
  AND (r.user_id IS NULL OR NOT EXISTS (SELECT 1 FROM users existing WHERE existing.id = r.user_id));
