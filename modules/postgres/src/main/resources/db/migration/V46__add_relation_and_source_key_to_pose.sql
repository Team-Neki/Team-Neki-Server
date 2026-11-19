-- BACKEND-276: 2인 포즈 관계 필터(친구/커플/썸)와 PM 일괄 업로드 멱등 키.
-- 전제: admin 계열(V41~V45)이 main 에 먼저 머지된다. staging/prod 는 out-of-order=false 라
-- 이 마이그레이션이 먼저 적용되면 나중에 오는 V45 가 거부된다.
-- 쓰지 않는 view_count 컬럼(V13)도 함께 지운다.
-- relation 은 2인 포즈에만 붙는다. 기존 포즈와 관계 없는 2인 포즈는 NULL 이며 '전체' 에서만 보인다.
-- source_key 는 PM 원본 키(pose.csv media_key)다. 수동 업로드는 NULL 이고, UNIQUE 는 NULL 을 여러 개 허용한다.
ALTER TABLE TB_POSE ADD COLUMN relation VARCHAR(30) NULL;
ALTER TABLE TB_POSE ADD COLUMN source_key VARCHAR(100) NULL;

ALTER TABLE TB_POSE
    ADD CONSTRAINT ck_pose_relation_value CHECK (relation IS NULL OR relation IN ('FRIEND', 'COUPLE', 'SSUM'));
ALTER TABLE TB_POSE
    ADD CONSTRAINT ck_pose_relation_two_only CHECK (relation IS NULL OR head_count = 'TWO');
ALTER TABLE TB_POSE
    ADD CONSTRAINT uk_pose_source_key UNIQUE (source_key);

ALTER TABLE TB_POSE DROP COLUMN view_count;

COMMENT ON COLUMN TB_POSE.relation IS '2인 포즈 관계 (FRIEND/COUPLE/SSUM). 2인만 허용, 기존 포즈는 NULL';
COMMENT ON COLUMN TB_POSE.source_key IS 'PM 원본 키 (pose.csv media_key). 일괄 업로드 멱등 판단용. 수동 업로드는 NULL';
