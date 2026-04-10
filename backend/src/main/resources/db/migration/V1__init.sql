CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner VARCHAR(128) NOT NULL,
  category VARCHAR(32) NOT NULL,
  reference_type VARCHAR(64) NOT NULL,
  reference_id VARCHAR(128) NOT NULL,
  filename TEXT NOT NULL,
  content_type VARCHAR(128),
  size BIGINT NOT NULL,
  checksum VARCHAR(64) NOT NULL,
  data BYTEA NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_files_reference
  ON files(reference_type, reference_id, category);

CREATE TABLE IF NOT EXISTS resources (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  type VARCHAR(32),
  summary TEXT,
  tags TEXT,
  external_url TEXT,
  object_key VARCHAR(256),
  visibility VARCHAR(32),
  status VARCHAR(32),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
