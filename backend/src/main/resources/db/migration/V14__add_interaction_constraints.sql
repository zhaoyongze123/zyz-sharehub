DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_comments_target_exactly_one'
  ) THEN
    ALTER TABLE comments
      ADD CONSTRAINT chk_comments_target_exactly_one
      CHECK (
        (resource_id IS NOT NULL AND note_id IS NULL)
        OR
        (resource_id IS NULL AND note_id IS NOT NULL)
      );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_favorites_target_exactly_one'
  ) THEN
    ALTER TABLE favorites
      ADD CONSTRAINT chk_favorites_target_exactly_one
      CHECK (
        (resource_id IS NOT NULL AND note_id IS NULL)
        OR
        (resource_id IS NULL AND note_id IS NOT NULL)
      );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_likes_target_exactly_one'
  ) THEN
    ALTER TABLE likes
      ADD CONSTRAINT chk_likes_target_exactly_one
      CHECK (
        (resource_id IS NOT NULL AND note_id IS NULL)
        OR
        (resource_id IS NULL AND note_id IS NOT NULL)
      );
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_likes_user_resource
  ON likes(user_key, resource_id)
  WHERE resource_id IS NOT NULL AND note_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_likes_user_note
  ON likes(user_key, note_id)
  WHERE note_id IS NOT NULL AND resource_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_favorites_user_resource
  ON favorites(user_key, resource_id)
  WHERE resource_id IS NOT NULL AND note_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_favorites_user_note
  ON favorites(user_key, note_id)
  WHERE note_id IS NOT NULL AND resource_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_comments_parent_created_at_id
  ON comments(parent_id, created_at, id);
