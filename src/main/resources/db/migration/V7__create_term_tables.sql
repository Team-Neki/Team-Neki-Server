-- Create term table
CREATE TABLE TB_TERM
(
    id            BIGSERIAL PRIMARY KEY,
    term_type     VARCHAR(50)  NOT NULL,
    title         VARCHAR(100) NOT NULL,
    url           VARCHAR(500) NOT NULL,
    version       VARCHAR(20)  NOT NULL,
    is_required   BOOLEAN      NOT NULL DEFAULT true,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

-- Add comments for term table
COMMENT ON TABLE TB_TERM IS '약관 정보 테이블';
COMMENT ON COLUMN TB_TERM.id IS '약관 고유 ID';
COMMENT ON COLUMN TB_TERM.term_type IS '약관 종류 (SERVICE, PRIVACY, LOCATION)';
COMMENT ON COLUMN TB_TERM.title IS '약관 제목';
COMMENT ON COLUMN TB_TERM.url IS '약관 상세 페이지 URL';
COMMENT ON COLUMN TB_TERM.version IS '약관 버전';
COMMENT ON COLUMN TB_TERM.is_required IS '필수 동의 여부';
COMMENT ON COLUMN TB_TERM.is_active IS '현재 활성 버전 여부';
COMMENT ON COLUMN TB_TERM.display_order IS '표시 순서';
COMMENT ON COLUMN TB_TERM.created_at IS '생성일시';
COMMENT ON COLUMN TB_TERM.updated_at IS '수정일시';

-- Insert initial term data
INSERT INTO TB_TERM (term_type, title, url, version, is_required, is_active, display_order, created_at, updated_at)
VALUES
    ('SERVICE', '서비스 이용약관', 'https://lydian-tip-26b.notion.site/2ee0d9441db0807c8684ce3e2d4b8aca', '1.0.0', true, true, 1, NOW(), NOW()),
    ('PRIVACY', '개인정보 수집 및 이용동의', 'https://lydian-tip-26b.notion.site/2ee0d9441db0807cb850f78145db6dd3', '1.0.0', true, true, 2, NOW(), NOW()),
    ('LOCATION', '위치정보 수집 및 이용 동의', 'https://lydian-tip-26b.notion.site/2ee0d9441db080b48223fb0b3263da08', '1.0.0', true, true, 3, NOW(), NOW());

-- Create user_term_agreement table
CREATE TABLE TB_USER_TERM_AGREEMENT
(
    user_id      BIGINT      NOT NULL,
    term_id      BIGINT      NOT NULL,
    agreed_at    TIMESTAMP   NOT NULL,
    term_version VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,

    PRIMARY KEY (user_id, term_id)
);

-- Add comments for user_term_agreement table
COMMENT ON TABLE TB_USER_TERM_AGREEMENT IS '사용자 약관 동의 기록 테이블';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.term_id IS '약관 ID';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.agreed_at IS '동의 일시';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.term_version IS '동의한 약관 버전';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.created_at IS '생성일시';
COMMENT ON COLUMN TB_USER_TERM_AGREEMENT.updated_at IS '수정일시';
