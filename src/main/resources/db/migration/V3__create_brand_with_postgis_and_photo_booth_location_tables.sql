-- Enable PostGIS extension for spatial data support

-- postgis는 일반 게정에서는 성정 못하므로 관리자 계정에서 직저 진행
-- CREATE
-- EXTENSION IF NOT EXISTS postgis;

-- Add comment
-- COMMENT ON EXTENSION postgis IS 'PostGIS extension for spatial data support';

-- Create brand table
CREATE TABLE TB_BRAND
(
    id BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    code       VARCHAR(30) NOT NULL,
    media_id   BIGINT      NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,

    CONSTRAINT uk_brand_name UNIQUE (name),
    CONSTRAINT uk_brand_code UNIQUE (code)
);

-- Add comments for brand table
COMMENT ON TABLE TB_BRAND IS '포토부스 브랜드 테이블';
COMMENT ON COLUMN TB_BRAND.id IS '브랜드 고유 ID';
COMMENT ON COLUMN TB_BRAND.name IS '브랜드 이름';
COMMENT ON COLUMN TB_BRAND.code IS '브랜드 코드';
COMMENT ON COLUMN TB_BRAND.created_at IS '생성일시';
COMMENT ON COLUMN TB_BRAND.updated_at IS '수정일시';

-- Insert initial brand data
INSERT INTO TB_BRAND (name, code, created_at, updated_at)
VALUES ('포토이즘', 'PHOTOISM', now(), now()),
       ('인생네컷', 'LIFEFOURCUTS', now(), now()),
       ('포토그레이', 'PHOTOGRAY', now(), now()),
       ('포토시그니처', 'PHOTOSIGNATURE', now(), now()),
       ('하루필름', 'HARUFILM', now(), now()),
       ('플랜비 스튜디오', 'PLANB_STUDIO', now(), now());


-- Create photo_booth_location table
CREATE TABLE TB_PHOTO_BOOTH_LOCATION
(
    id BIGSERIAL PRIMARY KEY,
    map_id      VARCHAR(100) NOT NULL,
    brand_id    BIGINT       NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    location    geometry(Point, 4326) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- Create spatial index for location column (for efficient spatial queries)
CREATE INDEX idx_photo_booth_location_location ON TB_PHOTO_BOOTH_LOCATION USING GIST (location);


-- Add comments for photo_booth_location table
COMMENT ON TABLE TB_PHOTO_BOOTH_LOCATION IS '포토부스 위치 정보 테이블';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.id IS '포토부스 위치 고유 ID';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.map_id IS '카카오맵_고유 ID (중복체킹용)';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.brand_id IS '브랜드 ID (TB_BRAND 참조)';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.branch_name IS '포토부스 지점명';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.address IS '주소';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.location IS '위치 좌표 (SRID 4326, 경도/위도 순서)';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.created_at IS '생성일시';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.updated_at IS '수정일시';