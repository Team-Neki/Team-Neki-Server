CREATE TABLE TB_PHOTO_IMAGE_FOLDER (
    id BIGSERIAL PRIMARY KEY,
    photo_image_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_photo_image_folder UNIQUE (photo_image_id, folder_id)
);

CREATE INDEX idx_photo_image_folder_folder_id ON TB_PHOTO_IMAGE_FOLDER (folder_id);
