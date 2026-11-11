-- 앱 사용자 테이블과 기존 ADMIN 권한을 재사용한다. Google 로그인 동시 가입만 중복 방지한다.
CREATE UNIQUE INDEX ux_users_google_oid ON tb_users (oid)
    WHERE provider_type = 'GOOGLE' AND oid IS NOT NULL;

-- Spring Session JDBC PostgreSQL 스키마. admin만 사용하도록 테이블 이름을 분리한다.
CREATE TABLE admin_session (
    primary_id CHAR(36) NOT NULL PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100)
);
CREATE UNIQUE INDEX admin_session_session_id_idx ON admin_session (session_id);
CREATE INDEX admin_session_expiry_time_idx ON admin_session (expiry_time);
CREATE INDEX admin_session_principal_name_idx ON admin_session (principal_name);

CREATE TABLE admin_session_attributes (
    session_primary_id CHAR(36) NOT NULL REFERENCES admin_session(primary_id) ON DELETE CASCADE,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
