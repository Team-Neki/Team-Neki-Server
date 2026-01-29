-- Create app version table
CREATE TABLE TB_APP_VERSION
(
    id              BIGSERIAL PRIMARY KEY,
    platform        VARCHAR(20)  NOT NULL UNIQUE,
    min_version     VARCHAR(20)  NOT NULL,
    current_version VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comments for documentation
COMMENT ON TABLE TB_APP_VERSION IS '앱 버전 관리 테이블';
COMMENT ON COLUMN TB_APP_VERSION.id IS '고유 ID';
COMMENT ON COLUMN TB_APP_VERSION.platform IS '플랫폼 (ANDROID, IOS)';
COMMENT ON COLUMN TB_APP_VERSION.min_version IS '최소 지원 버전';
COMMENT ON COLUMN TB_APP_VERSION.current_version IS '현재 최신 버전';
COMMENT ON COLUMN TB_APP_VERSION.created_at IS '생성일시';
COMMENT ON COLUMN TB_APP_VERSION.updated_at IS '수정일시';

-- Insert initial data
INSERT INTO TB_APP_VERSION (platform, min_version, current_version, created_at, updated_at)
VALUES ('ANDROID', '1.0.0', '1.0.0', NOW(), NOW()),
       ('IOS', '1.0.0', '1.0.0', NOW(), NOW());