-- Create favorite map table
CREATE TABLE TB_FAVORITE_MAP
(
    user_id     BIGINT    NOT NULL,
    location_id BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,

    PRIMARY KEY (user_id, location_id)
);

-- Add comments for favorite_map table
COMMENT ON TABLE TB_FAVORITE_MAP IS '지도 즐겨찾기 테이블';
COMMENT ON COLUMN TB_FAVORITE_MAP.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_FAVORITE_MAP.location_id IS '포토부스 위치 ID (TB_PHOTO_BOOTH_LOCATION 테이블 참조)';
COMMENT ON COLUMN TB_FAVORITE_MAP.created_at IS '생성일시';
COMMENT ON COLUMN TB_FAVORITE_MAP.updated_at IS '수정일시';