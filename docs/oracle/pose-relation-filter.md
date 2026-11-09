# 오라클 : 포즈 관계 필터 · 최신순 고정 · PM 일괄 업로드 (BACKEND-276)

이 문서는 BACKEND-276 작업(위키 8-1)의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 사람이 로컬/배포 환경에서 확인합니다.
명령은 저장소 루트에서 그대로 실행합니다. 업로드 절차는 `docs/runbook/pose-bulk-upload.md` 를 따릅니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과 | `./gradlew test -q` 종료코드 0 |
| O-0-3 | auto | api bootJar 빌드 성공 | `./gradlew :apps:api:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.yaml' '*.sql' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |

## O-A. 코드와 설정

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | 포즈·미디어 직접 업로드·아키텍처 테스트 통과 : `./gradlew :apps:api:test --tests '*Pose*' --tests '*StoreMediaDirectly*' --tests '*ArchitectureRulesTest' -q` 종료코드 0 |
| O-A-2 | auto | 도메인 순수 로직 테스트 통과 : `./gradlew :domain:test --tests '*ImageHeaderParser*' --tests '*PoseRelation*' --tests '*PoseServiceBulkPlan*' -q` 종료코드 0 |
| O-A-3 | auto | 마이그레이션 V46 하나 : `ls modules/postgres/src/main/resources/db/migration \| grep -c '^V46__'` = 1 |
| O-A-4 | auto | 결과 코드 D-17, D-18 이 각각 하나 : `grep -Ec '"D-1[78]"' core/src/main/kotlin/com/neki/core/code/ResultCode.kt` = 2 |
| O-A-5 | auto | part 한도 설정 : `grep -c 'max-part-count: 100' apps/api/src/main/resources/application.yaml` = 1 |
| O-A-6 | auto | 413 D-17/D-18 판정 (Tomcat 이 part 수 초과를 감싸는 방식 포함) : `BulkUploadPosesLimitE2ETest` 통과 |
| O-A-7 | auto | multipart 는 핸들러가 인자를 풀 때만 파싱 : `grep -c 'resolve-lazily: true' apps/api/src/main/resources/application.yaml` = 1. 이 설정 아래에서 `BulkUploadPosesLimitE2ETest`(413 D-17/D-18)와 `BulkUploadPosesE2ETest` 통과 |
| O-A-8 | auto | 이미지 하나로 포즈가 두 건 생기지 않음 (공유 파일은 IMAGE_SHARED_BY_ROWS) : `PoseServiceBulkPlanTest`, `BulkUploadPosesE2ETest.sharedImageIsRejected` 통과 |
| O-A-9 | 범위 | 같은 source_key 동시 등록 경합(저장 실패 → 재조회 → ALREADY_REGISTERED) 재판정은 **유닛으로만 검증했다** (`BulkUploadPosesUseCaseTest` 의 경합·재조회 실패·discard 실패 케이스). 실제 동시 요청 E2E 나 로컬 재현은 하지 않았다 |

## O-L. 로컬 실데이터 검증 (manual)

전제: `docker compose up -d db db-init localstack redis`, `docker exec localstack awslocal s3 mb s3://yapp-local`,
`./gradlew :apps:api:bootRun` (local 프로필, JASYPT_PASSWORD 필요). PM 데이터는 `~/Downloads` 의 `pose.csv`, `media.csv`,
`neki_pose_all`(friend 51), `neki_pose_all 2`(ssum 29 + solo 1).

| id | 판정 |
|---|---|
| O-L-1 | `flyway_schema_history` 최신 version = 46. `\d tb_pose` 에 relation, source_key, ck_pose_relation_value, ck_pose_relation_two_only, uk_pose_source_key |
| O-L-2 | friend 001~029 (29장, 31 part) 업로드 : 200, created 29, skipped 124, failed 0, unmatched 0 |
| O-L-3 | friend 030~051 (22장) : created 22, ALREADY_REGISTERED 29, unmatched 0 |
| O-L-4 | friend 51장 단일 요청 (100.88MiB, 53 part) : 200, created 0, skipped 153 (ALREADY_REGISTERED 51 + IMAGE_NOT_PROVIDED 102), unmatched 0. FRIEND 51 그대로 |
| O-L-5 | ssum + solo 30장 : created 30, ALREADY_REGISTERED 51, IMAGE_NOT_PROVIDED 72. relation 별 FRIEND 51, SSUM 29, NULL 1 |
| O-L-6 | friend + ssum/solo 81장 (159MiB, 83 part) : 413 D-17 또는 연결 끊김. 데이터 변화 없음 |
| O-L-7 | 1KB 더미 101장 (CSV 2 포함 103 part) : 413 D-18. 데이터 변화 없음 |
| O-L-8 | 목록 : TWO+FRIEND size=100 → 51건, TWO+SSUM → 20건 hasNext=true, page=1 → 9건 false, TWO+COUPLE → 빈 목록, ONE+FRIEND → 빈 목록, ONE → 1건 relation=null, sortOrder=ASC 결과 = 생략 결과(내림차순), TWO+SSUM 첫 항목 = ssum_029, imageUrl → `200 image/png` |
| O-L-9 | localstack `pose/` 객체 81개, tb_media POSE·UPLOADED 81건, width/height 1024/1536 |
| O-L-10 | ROLE_USER 토큰으로 업로드 → 403 |
| O-L-11 | Swagger 에 JSON/multipart operation 노출 방식 기록 |

## O-D. 배포 전/후 (manual)

| id | 판정 |
|---|---|
| O-D-1 | **이 브랜치를 단독으로 staging 에 배포하지 않는다.** `deploy-api-staging.yml` 은 임의 ref 를 dispatch 할 수 있지만, BACKEND-83 계열(V41~V45)이 main 에 머지되기 전에는 이 브랜치로 실행하지 않는다. staging 에 V41~V43 만 있는 상태에서 V46 이 먼저 적용되면 이후 V44, V45 가 `out-of-order: false` 로 거부된다 |
| O-D-2 | 머지 직전 BACKEND-83 계열(V41~V45)이 main 에 머지되어 있음 (`ls modules/postgres/src/main/resources/db/migration \| grep -E '^V4[1-5]__'` 5줄) |
| O-D-3 | 배포 전 staging `select version, description, checksum from flyway_schema_history order by installed_rank;` 에 V41~V45 가 있고 V46 은 없음 |
| O-D-4 | 클라이언트 담당에게 `sortOrder` 무시(최신순 고정)와 응답 `relation` 추가를 공유함 |
| O-D-5 | 배포 후 PM 이 runbook 대로 staging 에 업로드해 O-L-2~5 와 같은 summary 를 받음 |

## 검증 기록

### 2026-11-09 로컬 (브랜치 `feat/BACKEND-276-pose-relation-filter`, 기준 origin/main `699cf7af`, 미커밋 작업본)

| id | 결과 | 근거 |
|---|---|---|
| O-0-1 | 통과 | `spotlessCheck` 종료코드 0 |
| O-0-2 | 통과 | `./gradlew test` 종료코드 0. domain 114, apps/batch 18, apps/api 768, apps/admin 5, 실패 0 |
| O-0-3 | 통과 | `:apps:api:bootJar` 종료코드 0 |
| O-A-1, O-A-2, O-A-6 | 통과 | api 대상 153건, domain 대상 21건 실패 0. D-18 은 MultipartException → IllegalStateException → FileCountLimitExceededException 체인으로 판정됨 |
| O-L-1 | 통과 | flyway 46, 컬럼 2개, CHECK 2개, UNIQUE 1개 확인 |
| O-L-2 | 통과 | 200, 61.07MB 전송, created 29, skipped 124 (IMAGE_NOT_PROVIDED), failed 0, unmatched 0 |
| O-L-3 | 통과 | created 22, ALREADY_REGISTERED 29, IMAGE_NOT_PROVIDED 102, unmatched 0 |
| O-L-4 | 통과 | 200, 105.80MB 전송, created 0, ALREADY_REGISTERED 51 + IMAGE_NOT_PROVIDED 102, unmatched 0. FRIEND 51 |
| O-L-5 | 통과 | created 30, ALREADY_REGISTERED 51, IMAGE_NOT_PROVIDED 72. FRIEND 51, SSUM 29, NULL 1 |
| O-L-6 | 통과 | poseCsv + 이미지 81 = 82 part, 413 `D-17` JSON 수신 (약 1.98MB 전송 후 응답, 0.01초). 연결 끊김 없음. 데이터 변화 없음 |
| O-L-7 | 통과 | 1KB 더미 101 + CSV 2 = 103 part, 413 `D-18`. 데이터 변화 없음 |
| O-L-8 | 통과 | FRIEND 51 (전부 FRIEND), SSUM 20/true → 9/false, COUPLE 0, ONE+FRIEND 0, ONE 1건 relation=null, relation=LOVER 400, ASC = 생략 결과(81건 내림차순), SSUM 첫 항목 ssum_029_M1-4_F1-1, imageUrl 200 image/png |
| O-L-9 | 통과 | localstack `pose/` 81개, tb_media POSE·UPLOADED 81건 1024x1536, friend_001 size 2209565 = 원본 파일 크기 |
| O-L-10 | 통과 | ROLE_USER 토큰: multipart 403 `D-995`, JSON 403 |
| O-L-11 | 기록 | `/v3/api-docs` 에서 두 핸들러가 operation 하나로 합쳐짐 (content: application/json, multipart/form-data). 두 핸들러가 같은 summary/description 상수를 쓰므로 합쳐진 operation 에 multipart 안내와 curl 예시가 노출됨. images 는 `array<binary>` 라 Swagger UI 에서 파일을 여러 개 추가할 수 있는 형태 (UI 직접 조작은 하지 않음) |

### 2026-11-09 구현 리뷰 MINOR 반영 후 재검증

| id | 결과 | 근거 |
|---|---|---|
| O-0-1 | 통과 | `./gradlew spotlessApply spotlessCheck` 종료코드 0 |
| O-0-2, O-0-3 | 통과 | `./gradlew :domain:test --rerun :apps:api:test --rerun :apps:batch:test --rerun :apps:admin:test --rerun :apps:api:bootJar` 종료코드 0. domain 118, apps/api 769, apps/batch 18, apps/admin 5, 실패 0 |
| O-A-7 | 통과 | `resolve-lazily: true` 적용 후 Limit E2E 4건, Bulk E2E 12건 통과. 로컬에서 20MB multipart 를 익명 허용 경로 `POST /api/auth/refresh` 로 보내면 413 이 아니라 415 (본문을 파싱하지 않음) |
| O-A-8 | 통과 | PoseServiceBulkPlanTest 12건 (공유 파일 4건 추가), Bulk E2E 공유 파일 케이스 |
| O-L-7 | 통과 | 103 part → 413 `D-18`. tb_pose, tb_media, S3 객체 수와 최대 id 변화 없음 (81/81/81) |
| O-L-4 | 통과 | friend 51장 단일 재전송 → 200, created 0, ALREADY_REGISTERED 81 + IMAGE_NOT_PROVIDED 72 (ssum/solo 등록 이후라 81). 데이터 변화 없음 |
| O-L-6 | 통과 | 82 part 159MiB → 413 `D-17` JSON (약 1.5MB 전송 후). 데이터 변화 없음 |
