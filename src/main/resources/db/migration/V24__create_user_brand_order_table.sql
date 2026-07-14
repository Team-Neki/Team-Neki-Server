-- Create user brand order table
CREATE TABLE TB_USER_BRAND_ORDER
(
    user_id    BIGINT    NOT NULL,
    brand_id   BIGINT    NOT NULL,
    sort_order INTEGER   NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    PRIMARY KEY (user_id, brand_id)
);

-- Lookup all of a user's brand orderings at once
CREATE INDEX idx_user_brand_order_user_id ON TB_USER_BRAND_ORDER (user_id);

-- Add comments for user_brand_order table
COMMENT ON TABLE TB_USER_BRAND_ORDER IS '사용자별 브랜드 정렬 순서 테이블';
COMMENT ON COLUMN TB_USER_BRAND_ORDER.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_USER_BRAND_ORDER.brand_id IS '브랜드 ID (TB_BRAND 테이블 참조)';
COMMENT ON COLUMN TB_USER_BRAND_ORDER.sort_order IS '정렬 순서 (0부터 시작하는 인덱스, 오름차순)';
COMMENT ON COLUMN TB_USER_BRAND_ORDER.created_at IS '생성일시';
COMMENT ON COLUMN TB_USER_BRAND_ORDER.updated_at IS '수정일시';