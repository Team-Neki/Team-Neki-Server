-- 기존 데이터 중 10자 초과 폴더명을 10자로 잘라냄
UPDATE TB_FOLDER SET name = LEFT(name, 10) WHERE LENGTH(name) > 10;

-- 폴더명 최대 길이를 10자로 제한
ALTER TABLE TB_FOLDER ALTER COLUMN name TYPE VARCHAR(10);
