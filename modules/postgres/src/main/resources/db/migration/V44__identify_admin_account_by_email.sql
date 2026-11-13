-- 관리자 로그인은 Google만 사용하므로 Google이 확인한 이메일로 계정을 식별한다.
-- 제공자 식별 키(oid)는 더 이상 쓰지 않는다.
DROP INDEX ux_users_admin_sso_oid;

UPDATE tb_users
SET oid = NULL
WHERE provider_type = 'ADMIN_SSO';

CREATE UNIQUE INDEX ux_users_admin_sso_email ON tb_users (email)
    WHERE provider_type = 'ADMIN_SSO' AND email IS NOT NULL;

-- 이전 principal 직렬화 형식의 세션은 새 코드에서 재사용하지 않는다.
DELETE FROM admin_session;
