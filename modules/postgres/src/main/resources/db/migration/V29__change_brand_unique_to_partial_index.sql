-- 소프트 삭제된 브랜드가 name/code를 영구 점유하지 않도록
-- 테이블 전체 unique 제약을 살아있는 행 대상 partial unique index로 교체
ALTER TABLE TB_BRAND DROP CONSTRAINT uk_brand_name;
ALTER TABLE TB_BRAND DROP CONSTRAINT uk_brand_code;

CREATE UNIQUE INDEX uk_brand_name ON TB_BRAND (name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_brand_code ON TB_BRAND (code) WHERE deleted_at IS NULL;
