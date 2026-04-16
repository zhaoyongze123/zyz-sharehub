ALTER TABLE resources
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE roadmaps
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE notes
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE roadmap_enrollments
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE roadmap_progress
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE roadmap_node_progress
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE comments
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE favorites
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE likes
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE reports
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE resources r
SET user_id = u.id
FROM users u
WHERE r.user_id IS NULL
  AND r.owner_key = u.login;

UPDATE roadmaps r
SET user_id = u.id
FROM users u
WHERE r.user_id IS NULL
  AND r.owner_key = u.login;

UPDATE notes n
SET user_id = u.id
FROM users u
WHERE n.user_id IS NULL
  AND n.owner_key = u.login;

UPDATE roadmap_enrollments e
SET user_id = u.id
FROM users u
WHERE e.user_id IS NULL
  AND e.user_key = u.login;

UPDATE roadmap_progress p
SET user_id = u.id
FROM users u
WHERE p.user_id IS NULL
  AND p.user_key = u.login;

UPDATE roadmap_node_progress p
SET user_id = u.id
FROM users u
WHERE p.user_id IS NULL
  AND p.user_key = u.login;

UPDATE comments c
SET user_id = u.id
FROM users u
WHERE c.user_id IS NULL
  AND c.author_key = u.login;

UPDATE favorites f
SET user_id = u.id
FROM users u
WHERE f.user_id IS NULL
  AND f.user_key = u.login;

UPDATE likes l
SET user_id = u.id
FROM users u
WHERE l.user_id IS NULL
  AND l.user_key = u.login;

UPDATE reports r
SET user_id = u.id
FROM users u
WHERE r.user_id IS NULL
  AND r.reporter_key = u.login;
