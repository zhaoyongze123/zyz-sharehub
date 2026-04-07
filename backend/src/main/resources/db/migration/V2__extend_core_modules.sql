CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  login VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(255),
  avatar_file_id UUID,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS roadmaps (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  visibility VARCHAR(32),
  status VARCHAR(32),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS roadmap_nodes (
  id BIGSERIAL PRIMARY KEY,
  roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
  parent_id BIGINT,
  title TEXT NOT NULL,
  order_no INTEGER,
  resource_id BIGINT,
  note_id BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS roadmap_progress (
  id BIGSERIAL PRIMARY KEY,
  roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
  user_key VARCHAR(128) NOT NULL,
  payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  UNIQUE (roadmap_id, user_key)
);

CREATE TABLE IF NOT EXISTS notes (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  content_md TEXT NOT NULL,
  visibility VARCHAR(32),
  status VARCHAR(32),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS comments (
  id BIGSERIAL PRIMARY KEY,
  resource_id BIGINT,
  note_id BIGINT,
  parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
  author_key VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS favorites (
  id BIGSERIAL PRIMARY KEY,
  resource_id BIGINT,
  note_id BIGINT,
  user_key VARCHAR(128) NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS likes (
  id BIGSERIAL PRIMARY KEY,
  resource_id BIGINT,
  note_id BIGINT,
  user_key VARCHAR(128) NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reports (
  id BIGSERIAL PRIMARY KEY,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  reporter_key VARCHAR(128) NOT NULL,
  reason VARCHAR(255),
  details TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  resolved_at TIMESTAMP WITHOUT TIME ZONE,
  resolved_by VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
  id BIGSERIAL PRIMARY KEY,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  operator_key VARCHAR(128) NOT NULL,
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_roadmap_nodes_roadmap_id ON roadmap_nodes(roadmap_id);
CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_resource_id ON comments(resource_id);
CREATE INDEX IF NOT EXISTS idx_reports_status_created_at ON reports(status, created_at DESC);
