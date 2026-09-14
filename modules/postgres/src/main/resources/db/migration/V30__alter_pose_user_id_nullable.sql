-- 관리자 업로드 포즈는 귀속 사용자가 없으므로 user_id 를 NULL 허용으로 변경한다
ALTER TABLE TB_POSE
    ALTER COLUMN user_id DROP NOT NULL;

COMMENT ON COLUMN TB_POSE.user_id IS '사용자 ID (관리자 업로드는 NULL)';
