CREATE TABLE IF NOT EXISTS admin_whitelist (
  id BIGSERIAL PRIMARY KEY,
  github_login VARCHAR(128) NOT NULL UNIQUE,
  role VARCHAR(32) NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_whitelist_role ON admin_whitelist(role);
