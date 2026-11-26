-- 사진 공유 링크. 공유 URL(/share/{token}) 은 object key 대신 추측 불가능한 토큰만 노출한다.
-- user_id, photo_id 는 다른 애그리거트라 V27 원칙대로 FK 없이 둔다.
-- 원본 사진 soft delete / media 삭제는 조회 시점에 재검증하므로 이 테이블을 따라 지우지 않는다.
CREATE TABLE TB_SHARE_LINK
(
    id         BIGSERIAL   PRIMARY KEY,
    token      VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    photo_id   BIGINT      NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    deleted_at TIMESTAMP   NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);

-- soft delete 된 행이 토큰을 영구 점유하지 않도록 partial unique index (V12, V29 패턴)
CREATE UNIQUE INDEX uk_share_link_token
    ON TB_SHARE_LINK (token)
    WHERE deleted_at IS NULL;

-- 같은 사진 재공유 시 살아있는 토큰 재사용 조회용
CREATE INDEX idx_share_link_user_photo_expires
    ON TB_SHARE_LINK (user_id, photo_id, expires_at)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE TB_SHARE_LINK IS '사진 공유 링크 (토큰 기반 공개 조회)';
COMMENT ON COLUMN TB_SHARE_LINK.id IS '공유 링크 ID';
COMMENT ON COLUMN TB_SHARE_LINK.token IS '공유 토큰 (SecureRandom 256bit, Base64 URL-safe 무패딩 43자)';
COMMENT ON COLUMN TB_SHARE_LINK.user_id IS '공유한 사용자 ID (사진 소유자)';
COMMENT ON COLUMN TB_SHARE_LINK.photo_id IS '공유 대상 사진 ID (TB_PHOTO_IMAGE.id)';
COMMENT ON COLUMN TB_SHARE_LINK.expires_at IS '만료 일시 (KST, 초 단위)';
COMMENT ON COLUMN TB_SHARE_LINK.deleted_at IS '삭제 일시 (soft delete)';
COMMENT ON COLUMN TB_SHARE_LINK.created_at IS '생성 일시';
COMMENT ON COLUMN TB_SHARE_LINK.updated_at IS '수정 일시';
