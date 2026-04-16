CREATE TABLE IF NOT EXISTS roadmap_enrollments (
  id BIGSERIAL PRIMARY KEY,
  roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
  user_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_at TIMESTAMP WITHOUT TIME ZONE,
  completed_at TIMESTAMP WITHOUT TIME ZONE,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT uk_roadmap_enrollments UNIQUE (roadmap_id, user_key),
  CONSTRAINT chk_roadmap_enrollments_status
    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'QUIT'))
);

CREATE INDEX IF NOT EXISTS idx_roadmap_enrollments_user_status_updated
  ON roadmap_enrollments(user_key, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_roadmap_enrollments_roadmap_user
  ON roadmap_enrollments(roadmap_id, user_key);
