INSERT INTO TB_PHOTO_IMAGE_FOLDER (photo_image_id, folder_id, created_at, updated_at)
SELECT id, folder_id, NOW(), NOW()
FROM TB_PHOTO_IMAGE
WHERE folder_id IS NOT NULL
  AND deleted_at IS NULL
ON CONFLICT (photo_image_id, folder_id) DO NOTHING;
