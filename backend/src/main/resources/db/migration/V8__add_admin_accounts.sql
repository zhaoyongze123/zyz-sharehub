CREATE TABLE IF NOT EXISTS admin_accounts (
  id BIGSERIAL PRIMARY KEY,
  user_login VARCHAR(128) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL,
  granted_by VARCHAR(128),
  granted_at TIMESTAMP WITHOUT TIME ZONE,
  revoked_by VARCHAR(128),
  revoked_at TIMESTAMP WITHOUT TIME ZONE,
  remark TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_accounts_login_status ON admin_accounts(user_login, status);
