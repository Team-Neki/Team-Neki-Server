-- V4: Add folder cover photo tracking fields for performance optimization
-- Description: Adds cover_photo_created_at column to enable write-time cover update strategy

-- 1. Add cover_photo_created_at column for concurrency control
ALTER TABLE TB_FOLDER
ADD COLUMN IF NOT EXISTS cover_photo_created_at TIMESTAMP NULL;

-- 2. Create index for efficient latest photo lookup per folder
CREATE INDEX IF NOT EXISTS idx_photo_image_user_folder_created
ON TB_PHOTO_IMAGE(user_id, folder_id, created_at DESC);

-- 3. Initialize existing folders with their latest photo as cover
UPDATE TB_FOLDER f
SET
    cover_photo_id = latest.photo_id,
    cover_photo_created_at = latest.created_at
FROM (
    SELECT DISTINCT ON (p.folder_id)
        p.folder_id,
        p.id AS photo_id,
        p.created_at
    FROM TB_PHOTO_IMAGE p
    WHERE p.folder_id IS NOT NULL
    ORDER BY p.folder_id, p.created_at DESC
) AS latest
WHERE f.id = latest.folder_id
  AND f.user_id = (SELECT user_id FROM TB_PHOTO_IMAGE WHERE id = latest.photo_id LIMIT 1);

-- 4. Add column comment for documentation
COMMENT ON COLUMN TB_FOLDER.cover_photo_created_at IS '커버 사진의 생성일시 (동시성 제어용)';
