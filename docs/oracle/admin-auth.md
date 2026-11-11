# 오라클 : admin 세션 인증 (BACKEND-83, #361)

이 문서는 #361 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 staging(dev) 배포 후 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다. staging 요청은 `http/admin-auth-*.http` 네 파일에 단계별로 있고, 각 요청의 `client.test` 가 판정을 대신합니다. Google 로그인(O-D-4·O-D-18)만 사람이 브라우저로 하고, 나머지는 에이전트가 이 문서 순서대로 실행합니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | admin 테스트 전부 통과, 수 >= 31 (Docker 필요. `AdminProductionSchemaTest` 가 PostGIS 에 V1~V44 를 적용) | `./gradlew :apps:admin:test -q` 종료코드 0 뒤 `grep -ho 'tests="[0-9]*"' apps/admin/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` |
| O-0-3 | auto | admin bootJar 빌드 성공 | `./gradlew :apps:admin:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.yaml' '*.sql' '*.http' '*.json' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-5 | auto | 세션 쿠키 파일이 커밋되지 않음 | `git ls-files http/http-client.private.env.json \| wc -l` = 0, `git check-ignore -q http/http-client.private.env.json` 종료코드 0 |

## O-A. 코드와 설정

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | 세션 인증·Google 로그인·DB 장애·아키텍처 규칙 테스트 통과 : `./gradlew :apps:admin:test --tests '*AdminAuthE2ETest' --tests '*AdminGoogleLoginE2ETest' --tests '*AdminAuthorizationDatabaseErrorTest' --tests '*AdminArchitectureRulesTest' -q` 종료코드 0 |
| O-A-2 | auto | admin 앱 경로가 모두 `/api` 아래에 있음 (이전 `/admin` 매핑 없음) : `grep -rnE '"/admin(/\|")' apps/admin/src/main \| wc -l` = 0 |
| O-A-3 | auto | admin 보안 체인에서 CSRF 를 끄고 로그아웃은 POST 만 받음 : `grep -c 'csrf { it.disable() }' apps/admin/src/main/kotlin/com/neki/admin/auth/infra/security/SessionSecurityConfig.kt` = 1, `grep -c 'matcher(HttpMethod.POST, "/api/auth/logout")' apps/admin/src/main/kotlin/com/neki/admin/auth/infra/security/SessionSecurityConfig.kt` = 1 |
| O-A-4 | auto | identity key 개념이 남지 않음 : `grep -rn 'identityKey' apps/admin/src domain/src/main \| wc -l` = 0 |
| O-A-5 | auto | Flyway 버전 중복 없음 : `ls modules/postgres/src/main/resources/db/migration \| sed -E 's/__.*//' \| sort \| uniq -d \| wc -l` = 0 |
| O-A-6 | auto | admin 이 Spring Data 리포지터리를 직접 두지 않음 : `test ! -e apps/admin/src/main/kotlin/com/neki/admin/media/infra` 종료코드 0 (application 계층의 우회는 O-A-1 의 ArchUnit 규칙이 막음) |

## O-D. staging 배포 후 검증

admin 은 외부 Ingress 가 없으므로 노드의 port-forward 를 SSH 터널로 로컬 8081 에 엽니다. staging Secret 의 `ADMIN_OAUTH_REDIRECT_URI` 가 `http://localhost:8081/api/login/oauth2/code/google` 이라 같은 주소로 브라우저 로그인까지 이어집니다.

### 전제

- GitOps #44(`admin-api-stg` Application)가 머지되고, `overlays/admin-api-stg/secret.yaml` 이 `staging` namespace 에 적용됨
- Google OAuth 클라이언트에 `http://localhost:8081/api/login/oauth2/code/google` 이 등록됨
- `ijhttp` 설치 : `brew install ijhttp`

### 준비

1. 마이그레이션 적용 : staging API 가 V44 를 먼저 적용해야 admin 이 기동합니다 (admin 은 Flyway validate 만 수행). `stg` 에 #361 을 병합해 push 한 뒤 `gh workflow run deploy-api-staging.yml --ref stg` 를 실행합니다.
2. admin 이미지 : Deploy Admin API 워크플로는 main 에 포함된 커밋만 빌드하므로 머지 전 브랜치는 로컬에서 빌드합니다. 노드 아키텍처(`kubectl get node -o jsonpath='{.items[0].status.nodeInfo.architecture}'`)에 맞춰 `--platform` 을 지정합니다.
   ```bash
   ./gradlew :apps:admin:bootJar
   TAG=stg-$(git rev-parse --short=7 HEAD)
   gh auth token | docker login ghcr.io -u "$(gh api user -q .login)" --password-stdin
   docker buildx build --platform linux/amd64 --build-arg APP_MODULE=admin -t ghcr.io/team-neki/neki-admin-api:$TAG --push .
   ```
   GitOps `overlays/admin-api-stg/kustomization.yaml` 의 `images[].newTag` 를 `$TAG` 로 올려 main 에 push 합니다.
3. 터널 : 비밀번호 인증이므로 사람이 입력하거나 스크래치패드의 `expect` 스크립트로 띄웁니다. 비밀번호는 명령줄·문서에 적지 않습니다.
   ```bash
   ssh -N -L 8081:localhost:8081 -p 2024 yapp@suitestudy.com \
     'kubectl -n staging port-forward svc/neki-admin-api-service 8081:8081'
   ```
4. DB 접속 : 노드에서 `sudo -u postgres psql -d dev_yapp` 로 들어갑니다. 아래 SQL 은 모두 이 세션에서 실행합니다.
5. 실행 : `ijhttp --env-file http/http-client.env.json --private-env-file http/http-client.private.env.json --env staging http/admin-auth-<단계>.http`. 출력의 `client.log` 값(userId, email, mediaId)을 다음 SQL 에 씁니다.

### 배포 확인

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-0a | manual | V41~V44 가 성공으로 기록됨 | `SELECT version, success FROM flyway_schema_history WHERE version IN ('41','42','43','44') ORDER BY installed_rank;` 에서 4행 모두 `success = t` |
| O-D-0b | manual | admin Pod 가 새 이미지로 Ready | 노드에서 `kubectl -n staging get deploy neki-admin-api -o jsonpath='{.spec.template.spec.containers[0].image} {.status.readyReplicas}'` = `ghcr.io/team-neki/neki-admin-api:<TAG> 1` |

### 1단계 : 로그인 없이 (`http/admin-auth-1-anonymous.http`)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-1 | manual | 익명 me 는 401 D-999, 로그인 화면으로 리다이렉트하지 않음 | O-D-1 통과 |
| O-D-2 | manual | 로그인 시작은 302 로 Google 이동, state·nonce·터널 콜백 주소 포함, `ADMINSESSION` 발급 | O-D-2 통과 |
| O-D-3 | manual | 익명 업로드 티켓 요청은 401 | O-D-3 통과 |

### 2단계 : 최초 로그인 (`http/admin-auth-2-pending.http`)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-4 | manual (사람) | Google 로그인 후 세션 쿠키 확보 | 브라우저에서 `http://localhost:8081/api/oauth2/authorization/google` 로 로그인. `/api/auth/me` JSON 이 보이면 개발자 도구 Application → Cookies 의 `ADMINSESSION` 값을 `http/http-client.private.env.json` 의 `staging.adminSession` 에 넣음. 이미 ADMIN 인 계정이면 O-D-13 의 회수 SQL 을 먼저 실행 |
| O-D-5 | manual | 최초 로그인 사용자는 `admin=false`, 응답에 `X-CSRF-TOKEN` 헤더 없음 | O-D-5 통과. 출력의 userId·email 기록 |
| O-D-6 | manual | ADMIN 없는 관리 요청은 403 | O-D-6 통과 |
| O-D-7 | manual | 계정이 이메일 기준 `ADMIN_SSO` 로 저장되고 `oid` 는 비어 있음. 이후 ADMIN 부여 | `\set uid <userId>` 뒤 `SELECT provider_type, oid, email, role FROM tb_users WHERE id = :uid;` = `ADMIN_SSO`, NULL, O-D-5 의 email, `ROLE_USER`. 확인 후 `docs/runbook/admin-auth.md` 의 "수동 권한 부여" SQL 로 ADMIN 부여 (`UPDATE 1`) |

### 3단계 : ADMIN 부여 후 (`http/admin-auth-3-admin.http`)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-8 | manual | 재로그인 없이 같은 세션에서 `admin=true` | O-D-8 통과 |
| O-D-9 | manual | CSRF 헤더 없이 업로드 티켓 발급 | O-D-9 통과. 출력의 mediaId 기록 |
| O-D-10 | manual | GET 로그아웃은 200 이 아님 | O-D-10 통과 |
| O-D-11 | manual | GET 로그아웃 시도 후에도 세션 유지 | O-D-11 통과 |
| O-D-12 | manual | 이전 `/admin/auth/me` 는 ADMIN 세션으로도 403 | O-D-12 통과 |
| O-D-13 | manual | 업로드 미디어는 업로더 소유·공동 관리 표시. 이후 ADMIN 회수 | `\set mid <mediaId>` 뒤 `SELECT owner_id, admin_managed, media_type, status FROM tb_media WHERE id = :mid;` = O-D-5 userId, `t`, `POSE`, `INITIATED`. 확인 후 `UPDATE tb_users SET role = 'ROLE_USER', updated_at = CURRENT_TIMESTAMP WHERE id = :uid AND provider_type = 'ADMIN_SSO' RETURNING id, role;` (`UPDATE 1`) |

### 4단계 : ADMIN 회수와 로그아웃 (`http/admin-auth-4-revoked.http`)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-14 | manual | 회수가 같은 세션의 다음 요청부터 반영돼 `admin=false` | O-D-14 통과 |
| O-D-15 | manual | 회수 후 관리 요청은 403 | O-D-15 통과 |
| O-D-16 | manual | POST 로그아웃은 CSRF 헤더 없이 200 D-0 | O-D-16 통과 |
| O-D-17 | manual | 로그아웃한 세션은 401 | O-D-17 통과 |

### 마무리

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-18 | manual (사람) | 같은 Google 계정으로 다시 로그인해도 계정이 하나뿐 | 브라우저로 다시 로그인한 뒤 `SELECT count(*) FROM tb_users WHERE provider_type = 'ADMIN_SSO' AND email = '<O-D-5 email>';` = 1 |
| O-D-19 | manual | 검증용 미디어 정리 | `DELETE FROM tb_media WHERE id = :mid AND status = 'INITIATED';` = `DELETE 1`. 계정은 이후 검증에 재사용하도록 남김 |
