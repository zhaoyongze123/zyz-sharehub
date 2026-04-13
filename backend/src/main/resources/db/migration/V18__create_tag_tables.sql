CREATE TABLE IF NOT EXISTS tags (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  slug VARCHAR(160) NOT NULL,
  type VARCHAR(32) NOT NULL DEFAULT 'CONTENT',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tags_slug
  ON tags(slug);

CREATE TABLE IF NOT EXISTS resource_tags (
  resource_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_resource_tags PRIMARY KEY (resource_id, tag_id),
  CONSTRAINT fk_resource_tags_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
  CONSTRAINT fk_resource_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS note_tags (
  note_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_note_tags PRIMARY KEY (note_id, tag_id),
  CONSTRAINT fk_note_tags_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
  CONSTRAINT fk_note_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resource_tags_tag_resource
  ON resource_tags(tag_id, resource_id);

CREATE INDEX IF NOT EXISTS idx_note_tags_tag_note
  ON note_tags(tag_id, note_id);

INSERT INTO tags (name, slug, type, status, created_at, updated_at)
SELECT DISTINCT
  trimmed_tag,
  lower(regexp_replace(trimmed_tag, '[^[:alnum:][:alpha:]-]+', '', 'g')),
  'CONTENT',
  'ACTIVE',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  SELECT trim(regexp_split_to_table(coalesce(r.tags, ''), ',')) AS trimmed_tag
  FROM resources r
) split_tags
WHERE trimmed_tag <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM tags existing
    WHERE existing.slug = lower(regexp_replace(trimmed_tag, '[^[:alnum:][:alpha:]-]+', '', 'g'))
  );

INSERT INTO resource_tags (resource_id, tag_id, created_at)
SELECT DISTINCT
  r.id,
  t.id,
  CURRENT_TIMESTAMP
FROM resources r
CROSS JOIN LATERAL regexp_split_to_table(coalesce(r.tags, ''), ',') AS raw_tag(tag_name)
JOIN tags t
  ON t.slug = lower(regexp_replace(trim(raw_tag.tag_name), '[^[:alnum:][:alpha:]-]+', '', 'g'))
WHERE trim(raw_tag.tag_name) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM resource_tags existing
    WHERE existing.resource_id = r.id
      AND existing.tag_id = t.id
  );
