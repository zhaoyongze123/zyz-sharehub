CREATE INDEX IF NOT EXISTS idx_favorites_user_key ON favorites(user_key);
CREATE INDEX IF NOT EXISTS idx_resources_owner_key_status ON resources(owner_key, status);
CREATE INDEX IF NOT EXISTS idx_notes_owner_key_status ON notes(owner_key, status);
CREATE INDEX IF NOT EXISTS idx_resumes_owner_key_status ON resumes(owner_key, status);
