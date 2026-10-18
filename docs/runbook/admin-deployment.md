# Admin API 배포

`deploy-admin.yml`은 수동 실행으로 `apps/admin`을 테스트하고 공용 Dockerfile의
`APP_MODULE=admin`으로 `ghcr.io/team-neki/neki-admin-api:sha-<전체 커밋 SHA>`를
게시한다. 선택한 ref의 실제 체크아웃 SHA를 사용한다.

GitOps의 `overlays/admin-api/deployment.yaml` 이미지 줄이 정확히 하나인지 빌드 전에
확인하고, 이미지 push 후 별도 runner에서 해당 줄만 갱신한다. 빌드하는 코드에는
`GITOPS_PAT`를 제공하지 않는다. `GITHUB_TOKEN`에는 이미지 push를 위한
`packages: write`, `GITOPS_PAT`에는 Team-Neki-GitOps main push 권한이 필요하다.
동시 실행은 직렬화하며 대기 run은 GitHub 정책에 따라 새 요청으로 대체될 수 있다.

## 최초 실행

1. GitOps의 `admin-api` Application/overlay PR을 먼저 main에 머지한다.
2. 이 워크플로를 main에 머지한다. Actions → **Deploy Admin API (GHCR + GitOps)**
   → Run workflow에서 실행한다. `ref`를 비우면 워크플로를 실행한 ref를 사용한다.
3. 첫 push 후 GHCR `neki-admin-api` 패키지를 public으로 설정한다.
   GitOps 구성에는 imagePullSecret이 없으므로 private이면 pull에 실패한다.
4. GitOps 이미지 태그 갱신을 확인한 뒤 `kubectl apply -f argocd/apps/admin-api.yaml`로
   Application을 등록하고 rollout을 확인한다. `bootstrap` 태그는 자리표시자다.

GitOps 커밋 성공은 ArgoCD sync/rollout 성공을 보장하지 않는다. GitOps README의
`kubectl rollout status`와 port-forward 확인 절차로 클러스터 상태를 확인한다.
롤백은 `ref`에 이전 커밋 SHA를 입력해 같은 워크플로를 다시 실행한다.

## 현재 앱의 제한

GitOps는 `prod` namespace에 `replicas: 1` / `Recreate`, 내부 ClusterIP Service
(8081)로 배포한다. admin은 아직 `local` 메모리 H2 구성뿐이므로 해당 프로파일을
명시하고 H2 콘솔은 비활성화한다. 재시작/재배포 시 데이터는 초기화된다.
운영 PostgreSQL 연결은 별도 작업이다.

`AdminApplication`은 Spring Security 자동설정을 제외하고 있다. 외부 Ingress는
만들지 않지만 내부 Service 접근에 인증이 추가되는 것은 아니다. 인증 구현 및
어드민 웹의 백엔드 연결은 별도 작업이다.
