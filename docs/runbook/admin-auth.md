# Admin Google 로그인과 세션 운영

admin은 Google OIDC로 사용자를 식별하고 기존 `TB_USERS.role`의 `ROLE_ADMIN`을 매 관리 요청마다 조회합니다. 로그인 세션이 유효하면 DB의 권한 부여와 회수가 다음 요청에 반영됩니다. 세션은 Spring Session JDBC `ADMIN_SESSION`에 저장하며 유휴 만료는 30분입니다.

## 로컬 실행

1. API가 사용하는 PostgreSQL을 준비하고 API의 기존 Flyway 배포 경로로 V41까지 적용합니다. `apps/admin`은 Flyway validate만 수행하며 미적용 마이그레이션이 있으면 시작하지 않습니다.
2. Google Console에 Web application 클라이언트를 만들고 redirect URI `http://localhost:8081/login/oauth2/code/google`을 등록합니다.
3. 다음 환경변수를 제공하고 admin을 실행합니다. 로컬 DataSource 기본값은 `modules/postgres` 설정을 따릅니다. 다른 개발 DB를 쓸 때는 Spring DataSource 환경변수 세 개를 모두 지정합니다.

   ```sh
   export JASYPT_PASSWORD='기존 API 환경의 Jasypt 키'
   export GOOGLE_CLIENT_ID='실제 Google Web client ID'
   export GOOGLE_CLIENT_SECRET='실제 Google client secret'
   export ADMIN_OAUTH_REDIRECT_URI='http://localhost:8081/login/oauth2/code/google'
   ./gradlew :apps:admin:bootRun
   ```

4. 브라우저에서 `http://localhost:8081/oauth2/authorization/google`로 이동합니다. 인증 성공 후 `/admin/v1/auth/me`에 사용자·현재 권한·CSRF 토큰이 표시됩니다. 최초 Google 사용자는 ADMIN이 없습니다. 이메일이 같아도 다른 제공자의 계정과 합치지 않습니다.

운영에서는 `prod` 프로파일, HTTPS와 Secret의 실제 DataSource/OAuth 값을 사용합니다. 로컬 이외 프로파일은 Secure 쿠키를 사용합니다. 토큰·세션 쿠키를 로그나 URL에 넣지 않습니다.

## 프론트 연결 계약

`GET /admin/v1/auth/me`의 `data`는 다음 필드를 반환합니다.

| 필드 | 의미 |
|---|---|
| `userId`, `email`, `name` | 현재 Google 로그인에 연결된 기존 사용자 정보 |
| `admin` | 현재 DB에 ADMIN 권한이 있는지 |
| `csrfToken`, `csrfHeaderName` | 변경 요청과 로그아웃에 사용할 토큰 및 헤더 이름 |

변경 요청은 세션 쿠키와 `X-CSRF-TOKEN`을 함께 전송합니다. 토큰은 클라이언트 메모리에 보관하고 새로고침 시 me에서 다시 받습니다. 세션이 만료되면 다시 로그인합니다. `POST /admin/v1/auth/logout`은 CSRF 검증 후 세션을 무효화하고 HTTP 200 `BaseResponse`를 반환합니다.

Next는 로그인 시작·콜백·me·관리 API를 같은 외부 origin으로 중계해야 합니다. 모든 `Set-Cookie`와 상대 `Location`, 클라이언트가 전송한 Cookie·CSRF 헤더를 보존하고 서버 fetch가 OAuth 리다이렉트를 자동 추적하지 않게 합니다. Next가 임의 요청에 CSRF 토큰을 대신 발급해 붙이지 않습니다. 프록시 실제 경로와 성공 후 UI 이동은 Next 연결 시 확정합니다.

## 수동 권한 부여

아래는 psql 예시입니다. 운영자가 내부 사용자 ID와 `provider_type=GOOGLE`, Google `oid`를 확인한 뒤 지정합니다. 이메일만으로 다른 계정을 연결하거나 일괄 승인하지 않습니다.

```sql
\set admin_user_id 123
BEGIN;
SELECT id, email, provider_type, oid, role
FROM tb_users
WHERE id = :'admin_user_id'::bigint AND provider_type = 'GOOGLE' AND oid IS NOT NULL
FOR UPDATE;

UPDATE tb_users AS u
SET role = CASE
    WHEN EXISTS (
        SELECT 1 FROM unnest(string_to_array(u.role, ',')) AS r(value)
        WHERE btrim(r.value) = 'ROLE_ADMIN'
    ) THEN u.role
    ELSE concat_ws(',', nullif(u.role, ''), 'ROLE_ADMIN')
END,
updated_at = CURRENT_TIMESTAMP
WHERE id = :'admin_user_id'::bigint AND provider_type = 'GOOGLE' AND oid IS NOT NULL
RETURNING id, email, role;
-- 대상과 UPDATE 1 확인 후 COMMIT. 예상과 다르면 ROLLBACK.
COMMIT;
```

권한을 부여받은 사용자는 기존 세션으로 me를 새로 조회하거나 관리 API를 호출하면 됩니다. 별도 승인 테이블이나 새 로그인이 필요하지 않습니다. 처리자·대상·시각·변경 전후 권한을 운영 기록에 남깁니다.

## 권한 회수

```sql
\set admin_user_id 123
BEGIN;
SELECT id, email, role FROM tb_users
WHERE id = :'admin_user_id'::bigint AND provider_type = 'GOOGLE' FOR UPDATE;
UPDATE tb_users AS u
SET role = array_to_string(ARRAY(
    SELECT btrim(r.value) FROM unnest(string_to_array(u.role, ',')) AS r(value)
    WHERE btrim(r.value) <> 'ROLE_ADMIN'
), ','),
updated_at = CURRENT_TIMESTAMP
WHERE id = :'admin_user_id'::bigint AND provider_type = 'GOOGLE'
RETURNING id, email, role;
-- 대상과 UPDATE 1 확인 후 COMMIT. 예상과 다르면 ROLLBACK.
COMMIT;
```

기존 다른 권한은 유지하고 ADMIN만 제거합니다. 회수 커밋 후 인가 조회를 시작하는 관리 요청부터 HTTP 403을 반환합니다. 이미 인가를 통과해 진행 중인 요청은 중단하지 않습니다.

## 배포 순서

1. 실행 모듈 #356과 이 인증 변경의 공유 V41 마이그레이션을 main에 반영하고 기존 API 배포로 DB에 적용합니다.
2. workflow #357을 통해 인증 변경이 포함된 admin 이미지를 GHCR에 게시합니다.
3. GitOps #43의 `overlays/admin-api/secret.example.yaml`을 기준으로 실제 Secret을 별도로 적용합니다. Google client ID/secret과 외부 redirect URI가 필요합니다. DB는 API와 같은 공통 설정을 쓰고 기존 `neki-secrets`의 Jasypt 키로 복호화합니다. 실제 Secret은 커밋하지 않습니다.
4. GHCR 패키지 접근과 GitOps 이미지 태그를 확인한 뒤 `admin-api` Application을 등록합니다. `replicas: 1`, RollingUpdate, 내부 ClusterIP Service를 사용합니다.
5. 미인증 me 401, Google 로그인, DB ADMIN 부여 후 같은 쿠키로 접근, 회수 후 403, CSRF와 로그아웃을 확인합니다. Next UI 연결 전에는 Spring 직접 콜백 개발을 검증하고, 운영 브라우저 접근은 외부 HTTPS 경로 연결 후 확인합니다.

DDL은 API가 소유하며 admin은 테이블을 생성·변경하지 않습니다. 세션을 메모리 저장소로 대체하지 않습니다. 데이터베이스 마이그레이션을 유지한 채 인증 이전의 H2 이미지로 되돌리는 것은 운영 롤백 절차로 사용하지 않습니다.

## 테스트

기본 `./gradlew :apps:admin:test`는 H2 JDBC 세션과 로컬 OIDC 서버를 사용합니다. 실제 Google 자격증명이나 외부 Google 호출이 필요하지 않습니다. PostgreSQL partial unique index에 의존하는 동시 가입 테스트 한 개는 기본 실행에서 제외됩니다.

실제 PostgreSQL 검증은 비어 있는 **테스트 전용 DB**에 사용자 V1·이름 길이 V14·신규 V41만 먼저 적용한 뒤 실행합니다. 공간 엔티티를 포함한 전체 운영 스키마의 업그레이드 검증과는 구분합니다. 다음 변수는 준비한 테스트 DB와 role에 맞게 지정합니다.

```sh
export ADMIN_TEST_DATABASE_URL='postgresql://127.0.0.1:55483/admin_auth_test'
export SPRING_DATASOURCE_USERNAME='admin'
export SPRING_DATASOURCE_PASSWORD=''
psql "$ADMIN_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f modules/postgres/src/main/resources/db/migration/V1__create_users_table.sql
psql "$ADMIN_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f modules/postgres/src/main/resources/db/migration/V14__alter_users_name_length_to_10.sql
psql "$ADMIN_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f modules/postgres/src/main/resources/db/migration/V41__create_admin_sessions_and_google_identity.sql
SPRING_DATASOURCE_URL="jdbc:$ADMIN_TEST_DATABASE_URL" \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect \
SPRING_JPA_HIBERNATE_DDL_AUTO=none \
SPRING_SQL_INIT_MODE=never \
./gradlew :apps:admin:test --rerun --no-daemon
```

검증 범위는 동일 세션 권한 부여·회수, 정확한 ADMIN 매칭, 계정 삭제·신원 변경, CSRF와 로그아웃, 30분 만료, 동시 최초 가입, 실제 code 교환·RSA 서명·nonce 검증, 사용자 집계 분리입니다.
