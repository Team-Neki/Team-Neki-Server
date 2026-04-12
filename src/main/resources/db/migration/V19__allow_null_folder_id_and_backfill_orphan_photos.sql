-- 1. folder_id NULL 허용으로 변경
ALTER TABLE tb_photo_image_folder
    ALTER COLUMN folder_id DROP NOT NULL;

-- 2. 기존 unique constraint 삭제 (photo_image_id, folder_id 조합)
-- folder_id가 NULL이면 unique 동작이 달라지므로 삭제
ALTER TABLE tb_photo_image_folder
    DROP CONSTRAINT IF EXISTS uk_photo_image_folder;

-- 3. photo_image에 있지만 photo_image_folder에 없는 것들 삽입
INSERT INTO tb_photo_image_folder (photo_image_id, folder_id, created_at, updated_at)
SELECT
    pi.id,
    NULL,
    pi.created_at,
    pi.updated_at
FROM tb_photo_image pi
WHERE NOT EXISTS (
    SELECT 1 FROM tb_photo_image_folder pif
    WHERE pif.photo_image_id = pi.id
)
  AND pi.deleted_at IS NULL;