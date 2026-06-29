-- Create notification table
CREATE TABLE TB_NOTIFICATION
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE,
    device_token VARCHAR(512) NOT NULL,
    push_agreed  BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comments for documentation
COMMENT ON TABLE TB_NOTIFICATION IS '사용자 알림 설정 테이블';
COMMENT ON COLUMN TB_NOTIFICATION.id IS '고유 ID';
COMMENT ON COLUMN TB_NOTIFICATION.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_NOTIFICATION.device_token IS '기기 푸시 알림 토큰 (FCM/APNs)';
COMMENT ON COLUMN TB_NOTIFICATION.push_agreed IS '푸시 알림 수신 동의 여부';
COMMENT ON COLUMN TB_NOTIFICATION.created_at IS '생성일시';
COMMENT ON COLUMN TB_NOTIFICATION.updated_at IS '수정일시';