-- BACKEND-244 (BACKEND-228 후속): V38 로 원천 키가 tb_brand.code 로 바뀌어 V32 의 tb_brand.platform 은 더 쓰지 않는다.
-- V38 배포가 끝나 platform 을 SELECT 하는 옛 pod 가 없으므로 여기서 지운다.
DROP INDEX IF EXISTS uk_brand_platform;
ALTER TABLE TB_BRAND DROP COLUMN platform;
