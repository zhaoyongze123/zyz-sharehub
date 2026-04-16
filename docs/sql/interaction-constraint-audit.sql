-- 1. likes 非法目标：双空 / 双非空
SELECT id, resource_id, note_id, user_key
FROM likes
WHERE (resource_id IS NULL AND note_id IS NULL)
   OR (resource_id IS NOT NULL AND note_id IS NOT NULL);

-- 2. favorites 非法目标：双空 / 双非空
SELECT id, resource_id, note_id, user_key
FROM favorites
WHERE (resource_id IS NULL AND note_id IS NULL)
   OR (resource_id IS NOT NULL AND note_id IS NOT NULL);

-- 3. comments 非法目标：双空 / 双非空
SELECT id, resource_id, note_id, author_key
FROM comments
WHERE (resource_id IS NULL AND note_id IS NULL)
   OR (resource_id IS NOT NULL AND note_id IS NOT NULL);

-- 4. 重复点赞：资源
SELECT user_key, resource_id, COUNT(*) AS duplicate_count
FROM likes
WHERE resource_id IS NOT NULL AND note_id IS NULL
GROUP BY user_key, resource_id
HAVING COUNT(*) > 1;

-- 5. 重复点赞：笔记
SELECT user_key, note_id, COUNT(*) AS duplicate_count
FROM likes
WHERE note_id IS NOT NULL AND resource_id IS NULL
GROUP BY user_key, note_id
HAVING COUNT(*) > 1;

-- 6. 重复收藏：资源
SELECT user_key, resource_id, COUNT(*) AS duplicate_count
FROM favorites
WHERE resource_id IS NOT NULL AND note_id IS NULL
GROUP BY user_key, resource_id
HAVING COUNT(*) > 1;

-- 7. 重复收藏：笔记
SELECT user_key, note_id, COUNT(*) AS duplicate_count
FROM favorites
WHERE note_id IS NOT NULL AND resource_id IS NULL
GROUP BY user_key, note_id
HAVING COUNT(*) > 1;
