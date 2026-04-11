BEGIN;

TRUNCATE TABLE
  admin_audit_logs,
  reports,
  likes,
  favorites,
  comments,
  roadmap_progress,
  roadmap_nodes,
  roadmaps,
  notes,
  resumes,
  resources,
  files,
  users
RESTART IDENTITY CASCADE;

COMMIT;
