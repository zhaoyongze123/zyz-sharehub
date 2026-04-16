CREATE TABLE IF NOT EXISTS roadmap_node_progress (
  id BIGSERIAL PRIMARY KEY,
  roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
  node_id BIGINT NOT NULL REFERENCES roadmap_nodes(id) ON DELETE CASCADE,
  user_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  completed_at TIMESTAMP WITHOUT TIME ZONE,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT uk_roadmap_node_progress UNIQUE (node_id, user_key),
  CONSTRAINT chk_roadmap_node_progress_status
    CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_roadmap_node_progress_user_roadmap_status
  ON roadmap_node_progress(user_key, roadmap_id, status);

CREATE INDEX IF NOT EXISTS idx_roadmap_node_progress_roadmap_user_status
  ON roadmap_node_progress(roadmap_id, user_key, status);
