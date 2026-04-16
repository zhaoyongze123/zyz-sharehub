CREATE INDEX IF NOT EXISTS idx_roadmaps_owner_status_id_desc
  ON roadmaps(owner_key, status, id DESC);

CREATE INDEX IF NOT EXISTS idx_notes_owner_status_id_desc
  ON notes(owner_key, status, id DESC);

CREATE INDEX IF NOT EXISTS idx_notes_visibility_status_pinned_id_desc
  ON notes(visibility, status, is_pinned DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_resources_owner_status_visibility_updated_desc
  ON resources(owner_key, status, visibility, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_resources_status_visibility_updated_desc
  ON resources(status, visibility, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_target_target_status_created_desc
  ON reports(target_type, target_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_status_target_type_id_desc
  ON reports(status, target_type, id DESC);
