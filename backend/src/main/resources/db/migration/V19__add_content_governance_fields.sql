ALTER TABLE resources
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(128),
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(128);

ALTER TABLE notes
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(128),
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(128);

ALTER TABLE roadmaps
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(128),
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(128);

UPDATE resources
SET published_at = COALESCE(published_at, updated_at, created_at)
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;

UPDATE notes
SET published_at = COALESCE(published_at, updated_at, created_at)
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;

UPDATE roadmaps
SET published_at = COALESCE(published_at, updated_at, created_at)
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;
