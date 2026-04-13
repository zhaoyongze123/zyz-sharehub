ALTER TABLE notes
  ADD COLUMN IF NOT EXISTS category VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_notes_owner_category
  ON notes(owner_key, category);
