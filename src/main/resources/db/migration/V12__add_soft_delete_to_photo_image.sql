-- deleted_at 컬럼 추가 (soft delete용)
ALTER TABLE TB_PHOTO_IMAGE ADD COLUMN deleted_at TIMESTAMP NULL;

-- 기존 unique constraint 제거
-- soft-deleted 레코드까지 포함하면 삭제 후 같은 미디어 재등록 시 unique violation 발생
ALTER TABLE TB_PHOTO_IMAGE DROP CONSTRAINT uk_photo_image_media_id;

-- Partial Unique Index로 교체 (deleted_at IS NULL인 레코드에만 unique 보장)
CREATE UNIQUE INDEX uk_photo_image_media_id
    ON TB_PHOTO_IMAGE (media_id)
    WHERE deleted_at IS NULL;