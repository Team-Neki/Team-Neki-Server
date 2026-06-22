-- Create notification history table (발송된 푸시 알림 내역)
CREATE TABLE TB_NOTIFICATION_HIST
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    body       VARCHAR(500) NOT NULL,
    link       VARCHAR(512),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 사용자별 최신순 조회(최근 알림 30건)를 위한 인덱스
CREATE INDEX idx_notification_history_user_id_created_at
    ON TB_NOTIFICATION_HIST (user_id, created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE TB_NOTIFICATION_HIST IS '발송된 푸시 알림 내역 테이블';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.id IS '고유 ID';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.user_id IS '수신 사용자 ID';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.type IS '알림 종류 코드 (예: ARCHIVE, MARKETING)';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.title IS '알림 제목';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.body IS '알림 내용';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.link IS '알림 탭 시 이동할 딥링크 (예: neki://archive/123)';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.created_at IS '발송(생성)일시';
COMMENT ON COLUMN TB_NOTIFICATION_HIST.updated_at IS '수정일시';