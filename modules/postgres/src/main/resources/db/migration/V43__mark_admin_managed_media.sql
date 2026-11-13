ALTER TABLE TB_MEDIA ADD COLUMN admin_managed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN TB_MEDIA.admin_managed IS '관리자 세션에서 업로드해 관리자들이 공동 관리하는 미디어';
