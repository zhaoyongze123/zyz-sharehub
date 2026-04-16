-- 任务 6 巡检 SQL
-- 用于核对 user_id 回填覆盖率与孤儿数据

SELECT 'resources' AS table_name,
       COUNT(*) AS total_rows,
       COUNT(*) FILTER (WHERE user_id IS NOT NULL) AS filled_user_id_rows,
       COUNT(*) FILTER (WHERE user_id IS NULL) AS null_user_id_rows,
       COUNT(*) FILTER (WHERE user_id IS NULL AND owner_key IS NOT NULL) AS unresolved_rows
FROM resources
UNION ALL
SELECT 'roadmaps',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND owner_key IS NOT NULL)
FROM roadmaps
UNION ALL
SELECT 'notes',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND owner_key IS NOT NULL)
FROM notes
UNION ALL
SELECT 'roadmap_enrollments',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND user_key IS NOT NULL)
FROM roadmap_enrollments
UNION ALL
SELECT 'roadmap_progress',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND user_key IS NOT NULL)
FROM roadmap_progress
UNION ALL
SELECT 'roadmap_node_progress',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND user_key IS NOT NULL)
FROM roadmap_node_progress
UNION ALL
SELECT 'comments',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND author_key IS NOT NULL)
FROM comments
UNION ALL
SELECT 'favorites',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND user_key IS NOT NULL)
FROM favorites
UNION ALL
SELECT 'likes',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND user_key IS NOT NULL)
FROM likes
UNION ALL
SELECT 'reports',
       COUNT(*),
       COUNT(*) FILTER (WHERE user_id IS NOT NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL),
       COUNT(*) FILTER (WHERE user_id IS NULL AND reporter_key IS NOT NULL)
FROM reports;

SELECT 'resources_orphans' AS metric, COUNT(*) AS orphan_rows
FROM resources r
LEFT JOIN users u ON u.id = r.user_id
WHERE r.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'roadmaps_orphans', COUNT(*)
FROM roadmaps r
LEFT JOIN users u ON u.id = r.user_id
WHERE r.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'notes_orphans', COUNT(*)
FROM notes n
LEFT JOIN users u ON u.id = n.user_id
WHERE n.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'roadmap_enrollments_orphans', COUNT(*)
FROM roadmap_enrollments e
LEFT JOIN users u ON u.id = e.user_id
WHERE e.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'roadmap_progress_orphans', COUNT(*)
FROM roadmap_progress p
LEFT JOIN users u ON u.id = p.user_id
WHERE p.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'roadmap_node_progress_orphans', COUNT(*)
FROM roadmap_node_progress p
LEFT JOIN users u ON u.id = p.user_id
WHERE p.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'comments_orphans', COUNT(*)
FROM comments c
LEFT JOIN users u ON u.id = c.user_id
WHERE c.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'favorites_orphans', COUNT(*)
FROM favorites f
LEFT JOIN users u ON u.id = f.user_id
WHERE f.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'likes_orphans', COUNT(*)
FROM likes l
LEFT JOIN users u ON u.id = l.user_id
WHERE l.user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'reports_orphans', COUNT(*)
FROM reports r
LEFT JOIN users u ON u.id = r.user_id
WHERE r.user_id IS NOT NULL AND u.id IS NULL;
