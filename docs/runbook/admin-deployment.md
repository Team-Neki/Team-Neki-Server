# Admin API 배포

`deploy-admin.yml`은 수동 실행으로 `apps/admin`을 테스트하고 공용 Dockerfile의
`APP_MODULE=admin`으로 `ghcr.io/team-neki/neki-admin-api:sha-<전체 커밋 SHA>-run-<실행 ID>-attempt-<시도 번호>`를
게시한다. 선택한 ref의 실제 체크아웃 SHA를 사용하며, 재실행해도 기존 이미지 태그를 덮어쓰지 않는다.

GitOps의 `overlays/admin-api/deployment.yaml` 이미지 줄이 정확히 하나인지 빌드 전에
확인하고, 이미지 push 후 별도 runner에서 해당 줄만 갱신한다. 빌드하는 코드에는
`ADMIN_GITOPS_PAT`를 제공하지 않는다. `GITHUB_TOKEN`에는 이미지 push를 위한
`packages: write`, `ADMIN_GITOPS_PAT`에는 Team-Neki-GitOps main push 권한이 필요하다.
`ADMIN_GITOPS_PAT`는 `admin-production` 보호 환경의 secret으로만 등록하고 필수 승인자를
설정한다. 해당 환경 secret이 없으면 배포는 실패한다. 선택한 ref는 main에 포함된
커밋이어야 하며 이전 main SHA로 롤백할 수 있다.
동시 실행은 직렬화하며 대기 run은 GitHub 정책에 따라 새 요청으로 대체될 수 있다.

## 최초 실행

1. 실행 모듈 #356과 세션 인증 #361을 머지하고 API를 먼저 배포해 공유 DB의 V41·V42·V43
   마이그레이션을 적용한다. GitOps의 `admin-api` Application/overlay PR을 main에 머지한다.
   `prod` namespace에는 기존 API와 동일한 `neki-secrets/jasypt-password` 및 Google OAuth
   설정을 담은 `neki-admin-api-secret`이 있어야 한다.
2. 이 워크플로를 main에 머지한다. Actions → **Deploy Admin API (GHCR + GitOps)**
   → Run workflow에서 실행한다. `ref`를 비우면 워크플로를 실행한 ref를 사용한다.
3. 첫 push 후 GHCR `neki-admin-api` 패키지를 public으로 설정한다.
   GitOps 구성에는 imagePullSecret이 없으므로 private이면 pull에 실패한다.
4. GitOps 이미지 태그 갱신을 확인한 뒤 `kubectl apply -f argocd/apps/admin-api.yaml`로
   Application을 등록하고 `kubectl rollout status deployment/neki-admin-api -n prod`로
   기동을 확인한다. `bootstrap` 태그는 자리표시자다. 브랜드·포즈 API 호출은 #358·#359 배포 후 확인한다.

GitOps 커밋 성공은 ArgoCD sync/rollout 성공을 보장하지 않는다. GitOps README의
rollout 확인 절차로 클러스터 상태를 확인한다.
롤백은 `ref`에 이전 커밋 SHA를 입력해 같은 워크플로를 다시 실행한다.

## 현재 앱의 제한

GitOps는 `prod` namespace에 `replicas: 1` / `RollingUpdate`, 내부 ClusterIP Service
(8081)로 배포한다. `prod` 프로파일과 API의 `JASYPT_PASSWORD` Secret을 사용해
동일한 PostgreSQL DB·스키마의 기존 테이블에 연결한다. admin은 Flyway 검증만
수행하며 테이블 생성·변경은 API가 담당한다. H2는 테스트에서만 사용한다.

`AdminApplication`은 OAuth2 로그인과 PostgreSQL JDBC 세션으로 관리 요청을 인증하고,
요청마다 현재 DB의 ADMIN 권한을 확인한다. 변경 요청에는 CSRF 토큰이 필요하다. 외부 Ingress는
아직 없으며 어드민 웹의 프록시 연결과 실제 Google 로그인은 별도 검증이 필요하다.
