CREATE INDEX IF NOT EXISTS idx_notes_owner_status_active_id_desc
  ON notes(owner_key, status, id DESC)
  WHERE deleted_at IS NULL;
