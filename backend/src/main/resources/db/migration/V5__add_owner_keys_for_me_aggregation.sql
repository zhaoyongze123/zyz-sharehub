ALTER TABLE resources ADD COLUMN IF NOT EXISTS owner_key VARCHAR(128) NOT NULL DEFAULT 'local-dev-user';
ALTER TABLE roadmaps ADD COLUMN IF NOT EXISTS owner_key VARCHAR(128) NOT NULL DEFAULT 'local-dev-user';
ALTER TABLE notes ADD COLUMN IF NOT EXISTS owner_key VARCHAR(128) NOT NULL DEFAULT 'local-dev-user';
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS owner_key VARCHAR(128) NOT NULL DEFAULT 'local-dev-user';

CREATE INDEX IF NOT EXISTS idx_resources_owner_key ON resources(owner_key);
CREATE INDEX IF NOT EXISTS idx_roadmaps_owner_key ON roadmaps(owner_key);
CREATE INDEX IF NOT EXISTS idx_notes_owner_key ON notes(owner_key);
CREATE INDEX IF NOT EXISTS idx_resumes_owner_key ON resumes(owner_key);
