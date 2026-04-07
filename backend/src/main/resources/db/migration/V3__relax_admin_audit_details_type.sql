ALTER TABLE admin_audit_logs
ALTER COLUMN details TYPE TEXT
USING details::text;
