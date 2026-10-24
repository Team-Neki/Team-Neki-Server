-- 기존 Google 관리 계정을 제공자 공통 키로 옮긴다.
ALTER TABLE tb_users ALTER COLUMN oid TYPE VARCHAR(512);

UPDATE tb_users
SET oid = CASE WHEN oid IS NULL THEN NULL ELSE 'google:' || oid END,
    provider_type = 'ADMIN_SSO'
WHERE provider_type = 'GOOGLE';

DROP INDEX ux_users_google_oid;
CREATE UNIQUE INDEX ux_users_admin_sso_oid ON tb_users (oid)
    WHERE provider_type = 'ADMIN_SSO' AND oid IS NOT NULL;

-- 이전 AdminPrincipal 직렬화 형식의 세션은 새 코드에서 재사용하지 않는다.
DELETE FROM admin_session;
