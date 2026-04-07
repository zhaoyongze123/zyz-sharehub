CREATE TABLE IF NOT EXISTS resumes (
  id BIGSERIAL PRIMARY KEY,
  template_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  file_id UUID NOT NULL REFERENCES files(id),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_resumes_status_created_at ON resumes(status, created_at DESC);
