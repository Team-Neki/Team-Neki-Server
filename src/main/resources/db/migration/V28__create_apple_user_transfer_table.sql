-- =====================================================================
-- Apple App Transfer 사용자 식별자 매핑 테이블
--   old_sub(A팀 sub) -> transfer_sub(이전용) -> new_sub(B팀 sub) 매핑 보관.
--   로그인 방어 로직이 transfer_sub 로 기존 사용자를 찾는 데 사용한다.
--   전환기(60일) 종료 및 검증 완료 후 DROP 하여 정리 가능.
--   user_id 는 같은 아그리게이트(user 모듈) 내부 참조이므로 FK 를 건다.
-- =====================================================================
CREATE TABLE TB_APPLE_USER_TRANSFER (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    old_sub      VARCHAR(255) NOT NULL,
    transfer_sub VARCHAR(255) NOT NULL,
    new_sub      VARCHAR(255) NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uq_apple_user_transfer_transfer_sub UNIQUE (transfer_sub),
    CONSTRAINT uq_apple_user_transfer_old_sub UNIQUE (old_sub),
    -- new_sub 은 배치가 채우기 전엔 NULL. PostgreSQL 은 NULL 을 서로 다르게 취급하므로
    -- 미채움 행은 제약을 위반하지 않고, 채워진 값끼리는 유일성이 보장된다.
    CONSTRAINT uq_apple_user_transfer_new_sub UNIQUE (new_sub),
    CONSTRAINT fk_apple_user_transfer_user FOREIGN KEY (user_id) REFERENCES TB_USERS (id) ON DELETE CASCADE
);

CREATE INDEX idx_apple_user_transfer_user_id ON TB_APPLE_USER_TRANSFER (user_id);

COMMENT ON TABLE TB_APPLE_USER_TRANSFER IS 'Apple App Transfer 사용자 식별자 매핑';
COMMENT ON COLUMN TB_APPLE_USER_TRANSFER.old_sub IS '이전 A팀 Apple sub (매핑 생성 시점 users.oid)';
COMMENT ON COLUMN TB_APPLE_USER_TRANSFER.transfer_sub IS 'Apple 이전용 식별자 (로그인 조회 키)';
COMMENT ON COLUMN TB_APPLE_USER_TRANSFER.new_sub IS '신규 B팀 Apple sub (exchange 결과)';
