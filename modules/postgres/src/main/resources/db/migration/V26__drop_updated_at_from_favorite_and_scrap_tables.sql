-- 즐겨찾기/스크랩 테이블은 생성일시만 관리하므로 updated_at 컬럼 제거
ALTER TABLE TB_FAVORITE_MAP DROP COLUMN updated_at;
ALTER TABLE TB_FAVORITE_IMAGE DROP COLUMN updated_at;
ALTER TABLE TB_SCRAP_POSE DROP COLUMN updated_at;