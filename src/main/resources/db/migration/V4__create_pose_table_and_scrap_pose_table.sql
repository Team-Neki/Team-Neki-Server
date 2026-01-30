-- Create pose table
CREATE TABLE TB_POSE
(
    id BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    media_id   BIGINT      NOT NULL,
    head_count VARCHAR(30) NOT NULL,
    memo       VARCHAR(255),
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);

-- Add comments for photo_image table
COMMENT
ON TABLE TB_POSE IS '포즈 저장 테이블';
COMMENT
ON COLUMN TB_POSE.id IS '포즈 고유 ID';
COMMENT
ON COLUMN TB_POSE.user_id IS '사용자 ID';
COMMENT
ON COLUMN TB_POSE.media_id IS '미디어 파일 ID (TB_MEDIA 테이블 참조)';
COMMENT
ON COLUMN TB_POSE.head_count IS '인원 수';
COMMENT
ON COLUMN TB_POSE.memo IS '메모';
COMMENT
ON COLUMN TB_POSE.created_at IS '생성일시';
COMMENT
ON COLUMN TB_POSE.updated_at IS '수정일시';

-- Create scrap_pose table
CREATE TABLE TB_SCRAP_POSE
(
    user_id    BIGINT    NOT NULL,
    pose_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    PRIMARY KEY (user_id, pose_id)
);

-- Add comments for scrap_pose table
COMMENT
ON TABLE TB_SCRAP_POSE IS '포즈 스크랩 테이블';
COMMENT
ON COLUMN TB_SCRAP_POSE.user_id IS '사용자 ID';
COMMENT
ON COLUMN TB_SCRAP_POSE.pose_id IS '포즈 ID';
COMMENT
ON COLUMN TB_SCRAP_POSE.created_at IS '생성일시';
COMMENT
ON COLUMN TB_SCRAP_POSE.updated_at IS '수정일시';
