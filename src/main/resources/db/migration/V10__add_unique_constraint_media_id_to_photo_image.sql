ALTER TABLE TB_PHOTO_IMAGE
    ADD CONSTRAINT uk_photo_image_media_id UNIQUE (media_id);
