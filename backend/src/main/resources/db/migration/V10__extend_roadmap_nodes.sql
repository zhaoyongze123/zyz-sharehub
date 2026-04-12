ALTER TABLE roadmap_nodes
  ADD COLUMN IF NOT EXISTS description TEXT;

CREATE INDEX IF NOT EXISTS idx_files_roadmap_node_reference
  ON files(reference_type, reference_id, category, created_at);
