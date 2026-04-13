CREATE TABLE IF NOT EXISTS note_view_history (
  id BIGSERIAL PRIMARY KEY,
  note_id BIGINT NOT NULL,
  user_key VARCHAR(128) NOT NULL,
  viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_note_view_history_note_user UNIQUE (note_id, user_key)
);

CREATE INDEX IF NOT EXISTS idx_note_view_history_user_viewed_at
  ON note_view_history(user_key, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_note_view_history_note_id
  ON note_view_history(note_id);
