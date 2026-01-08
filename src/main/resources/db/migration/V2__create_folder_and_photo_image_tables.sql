-- Create folder table
CREATE TABLE TB_FOLDER (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    cover_photo_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_folder_user_name UNIQUE (user_id, name)
);

-- Add comments for folder table
COMMENT ON TABLE TB_FOLDER IS '사용자 사진 폴더 테이블';
COMMENT ON COLUMN TB_FOLDER.id IS '폴더 고유 ID';
COMMENT ON COLUMN TB_FOLDER.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_FOLDER.name IS '폴더 이름';
COMMENT ON COLUMN TB_FOLDER.created_at IS '생성일시';
COMMENT ON COLUMN TB_FOLDER.updated_at IS '수정일시';

-- Create photo_image table
CREATE TABLE TB_PHOTO_IMAGE (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id VARCHAR(64) NOT NULL UNIQUE,
    folder_id BIGINT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comments for photo_image table
COMMENT ON TABLE TB_PHOTO_IMAGE IS '사용자 사진 이미지 테이블';
COMMENT ON COLUMN TB_PHOTO_IMAGE.id IS '사진 고유 ID';
COMMENT ON COLUMN TB_PHOTO_IMAGE.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_PHOTO_IMAGE.file_id IS '파일 고유 ID (S3 키 등)';
COMMENT ON COLUMN TB_PHOTO_IMAGE.folder_id IS '폴더 ID (nullable, 폴더 삭제 시 NULL)';
COMMENT ON COLUMN TB_PHOTO_IMAGE.status IS '업로드 상태 (INITIATED, UPLOADED, VERIFIED, FAILED)';
COMMENT ON COLUMN TB_PHOTO_IMAGE.created_at IS '생성일시';
COMMENT ON COLUMN TB_PHOTO_IMAGE.updated_at IS '수정일시';

-- Create media table
CREATE TABLE TB_MEDIA (
    id BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    content_type VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add index for media table
CREATE INDEX idx_media_owner_id ON TB_MEDIA(owner_id);
CREATE INDEX idx_media_storage_key ON TB_MEDIA(storage_key);

-- Add comments for media table
COMMENT ON TABLE TB_MEDIA IS '미디어 파일 테이블';
COMMENT ON COLUMN TB_MEDIA.id IS '미디어 고유 ID';
COMMENT ON COLUMN TB_MEDIA.storage_key IS '스토리지 키 (S3 키 등)';
COMMENT ON COLUMN TB_MEDIA.owner_id IS '소유자 ID';
COMMENT ON COLUMN TB_MEDIA.media_type IS '미디어 타입 (USER_PROFILE, PHOTO_BOOTH)';
COMMENT ON COLUMN TB_MEDIA.status IS '상태 (INITIATED, UPLOADED, FAILED, DELETE_REQUESTED, DELETED)';
COMMENT ON COLUMN TB_MEDIA.content_type IS '컨텐츠 타입 (image/jpeg, image/png 등)';
COMMENT ON COLUMN TB_MEDIA.created_at IS '생성일시';
COMMENT ON COLUMN TB_MEDIA.updated_at IS '수정일시';
