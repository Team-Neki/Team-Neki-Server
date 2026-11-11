# 오라클 : 포즈 관계 필터 · 최신순 고정 · sourceKey 기반 관리자 등록 (BACKEND-276)

이 문서는 BACKEND-276 작업(위키 8-1)의 완료 판정 기준을 다룹니다.
- 판정 방식: auto 는 명령이 판정하고, manual 은 사람이 로컬이나 배포 환경에서 확인합니다.
- 명령은 저장소 루트에서 그대로 실행합니다.
- 범위(v3)
  - V46 (relation, source_key 추가, 쓰지 않는 view_count 삭제)
  - `POST /api/poses/search` 의 filterGroup(headCountFilter, relationFilter) 과 최신순 고정. 기존 `GET /api/poses` 는 deprecated 로 남기고 relation 필터는 받지 않는다
  - JSON `POST /api/poses/admin/upload` 의 relation, sourceKey(409 D-18)
  - PM 데이터 적재 스크립트 `scripts/pose/upload_poses.py`
- multipart 일괄 업로드(v2)는 제거했습니다. 적재 절차는 `docs/runbook/pose-bulk-upload.md` 에 있습니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과 | `./gradlew test -q` 종료코드 0 |
| O-0-3 | auto | api bootJar 빌드 성공 | `./gradlew :apps:api:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.yaml' '*.sql' '*.py' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-5 | auto | 적재 스크립트 문법 | `python3 -m py_compile scripts/pose/upload_poses.py` 종료코드 0 |

## O-A. 코드와 설정

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | 포즈 테스트와 아키텍처 규칙 통과 : `./gradlew :apps:api:test --tests '*Pose*' --tests '*ArchitectureRulesTest' -q` 종료코드 0 |
| O-A-2 | auto | 관계 enum 테스트 통과 : `./gradlew :domain:test --tests '*PoseRelation*' -q` 종료코드 0 |
| O-A-3 | auto | 마이그레이션 V46 하나 : `ls modules/postgres/src/main/resources/db/migration \| grep -c '^V46__'` = 1 |
| O-A-4 | auto | 결과 코드 D-18 하나 : `grep -c '"D-18"' core/src/main/kotlin/com/neki/core/code/ResultCode.kt` = 1 |
| O-A-5 | auto | multipart(v2) 잔재 없음 : `rg -n "B[u]lkUpload\|P[o]seCsvParser\|I[m]ageHeader\|I[m]ageMetadata\|M[e]diaDirectUpload\|S[t]oreMedia\|D[i]scardMedia\|s[t]orePoseImage\|d[i]scardPoseImage\|f[a]ilPutWhen\|P[o]seImageFile\|P[o]seImageStoreOutcome\|I[M]AGE_SHARED_BY_ROWS\|U[P]LOAD_PAYLOAD_TOO_LARGE\|U[P]LOAD_TOO_MANY_PARTS\|m[a]x-part-count\|r[e]solve-lazily\|p[n]gHeaderBytes\|U[P]LOAD_SUMMARY" apps domain core modules docs scripts` 결과 0줄 (각 토큰의 두 번째 글자를 `[x]` 로 감싸 이 문서 자신은 일치하지 않게 했다. 정규식 의미는 같다) |
| O-A-6 | auto | 되돌린 파일이 main 과 같음 : 아래 8개 파일 각각 `git diff --quiet origin/main -- <file>` 종료코드 0 |
| O-A-7 | 범위 | 같은 sourceKey 를 동시에 등록할 때의 저장 경합 판정(저장 실패 → 롤백 → 재조회 → 409)은 **유닛으로만 검증했다** (`UploadPosesUseCaseTest` 의 경합, 재조회 결과 없음, 재조회 실패 케이스). 동시 요청 E2E 나 로컬 재현은 하지 않았다 |

O-A-6 대상 파일
- `apps/api/src/main/kotlin/com/neki/api/media/application/dto/MediaResult.kt`
- `apps/api/src/main/kotlin/com/neki/api/pose/infra/client/PoseMediaClient.kt`
- `domain/src/main/kotlin/com/neki/domain/pose/client/MediaClient.kt`
- `domain/src/main/kotlin/com/neki/domain/media/dto/MediaCommand.kt`
- `domain/src/main/kotlin/com/neki/domain/media/external/MediaStorage.kt`
- `domain/src/main/kotlin/com/neki/domain/media/infra/storage/s3/S3MediaStorageAdapter.kt`
- `domain/src/main/kotlin/com/neki/domain/media/infra/storage/fake/FakeMediaStorageConfig.kt`
- `apps/api/src/main/resources/application.yaml`

## O-L. 로컬 검증 (manual)

전제
- 인프라: `docker compose up -d db db-init localstack redis`, localstack 버킷 `yapp-local`
- 서버: `./gradlew :apps:api:bootRun` (local 프로필, JASYPT_PASSWORD 필요)
- PM 데이터: `~/Downloads` 의 `pose.csv`, `media.csv`, `neki_pose_all`(friend 51), `neki_pose_all 2`(ssum 29 + solo 1)

| id | 판정 |
|---|---|
| O-L-0 | flyway 최신 version 46, 스키마 변화 없음 |
| O-L-1 | 이전 multipart 데이터만 삭제한다. 대상은 `source_key IS NOT NULL` 포즈 81건, 그 POSE media, S3 객체다. DB 를 먼저 처리하고(`DELETE … RETURNING storage_key`, 81건 검사), 반환된 키만 S3 에서 지운다. 결과는 tb_pose 0, POSE media 0, S3 `pose/` 0 이고 다른 테이블은 그대로다 |
| O-L-3 | dry-run : rows 153, local_invalid 0, no_image 72, pending 81, 배치 9개 |
| O-L-4 | 적재 : registered 81, conflict 0, failed 0, api_calls ticket 9 / put 81 / admin_upload 9. FRIEND 51 / SSUM 29 / NULL 1, source_key 81 개 모두 다름, POSE media UPLOADED 81 (1024x1536), S3 81 |
| O-L-5 | 원장 재실행 : already_in_ledger 81, api_calls 0, DB·S3 변화 없음 |
| O-L-6a | 원장 없이 `--only 'friend_00[12]_*'` : conflict 2. 보고된 고아 mediaId 2개가 INITIATED 다. tb_pose 81 이고 기존 poseId 는 그대로다 |
| O-L-6b | ssum_029 1건을 지운 뒤 원장 없이 `--only 'ssum_02[89]_*'` : ssum_028 CONFLICT, ssum_029 REGISTERED, tb_pose 81 |
| O-L-6c | curl : 같은 sourceKey 2개는 400 D-01, `"a b"` 는 400 D-01, ONE+relation 은 400 D-01, USER 토큰은 403, 이미 등록된 키는 409 D-18. 모두 DB 변화가 없다 |
| O-L-6d | 고아 정리 : `status='INITIATED' AND media_type='POSE' AND NOT EXISTS(tb_pose 참조)` 조건으로 DB 를 먼저 지우고, 반환된 storage_key 만 S3 에서 지운다. 결과는 고아 0, POSE media 81, S3 81 |
| O-L-7 | 목록 결과(`POST /api/poses/search`, filterGroup) : TWO+FRIEND size=100 → 51건, TWO+SSUM → 20건(hasNext=true), page=1 → 9건(false), TWO+COUPLE → 빈 목록, ONE+FRIEND → 빈 목록, ONE → 1건(relation=null), deprecated GET 의 sortOrder=ASC 결과 = 생략 결과(내림차순), SSUM 첫 항목 = 다시 등록한 ssum_029, imageUrl → `200 image/png` |

## O-D. 배포 전/후 (manual)

| id | 판정 |
|---|---|
| O-D-1 | admin 계열(V41~V45)이 이 브랜치보다 **먼저** main 에 머지된다. 그래서 마이그레이션은 V46 이다. V45 가 main 에 들어오기 전에는 이 브랜치를 머지하지 않는다 |
| O-D-2 | 머지 직전 main 의 마지막 마이그레이션이 V45 다 (`git ls-tree --name-only origin/main modules/postgres/src/main/resources/db/migration/ \| xargs -n1 basename \| sort -V \| tail -1` 이 `V45__` 로 시작). ResultCode 의 D-18 이 main 의 다른 코드와 겹치지 않는다 |
| O-D-3 | 배포 전 staging `select version, description, checksum from flyway_schema_history order by installed_rank;` 에 main 의 V41~V45 가 있고 V46 은 없다. 이 브랜치의 이전 버전(V41 로 번호를 매겼던 때)이 적용된 기록이 있으면 먼저 정리한다 |
| O-D-4 | 클라이언트 담당에게 `sortOrder` 무시(최신순 고정)와 응답 `relation` 추가를 공유했다 |
| O-D-5 | 배포 후 staging 에서 스크립트로 소량(`--only`)을 적재해 registered 를 확인하고, 같은 원장으로 재실행해 already_in_ledger 를 확인한다 |

## 검증 기록

### 2026-11-09 v3 로컬 (브랜치 `feat/BACKEND-276-pose-relation-filter`, 3a10383d 위 미커밋 작업본)

| id | 결과 | 근거 |
|---|---|---|
| O-L-0 | 통과 | `Schema "public" is up to date`, version 46 |
| O-L-1 | 통과 | 사전 검사 81/81. DB 에서 반환된 키 81개를 받은 뒤 S3 를 지웠다. 결과 0/0, S3 0. tb_users 3 은 그대로 |
| O-L-3 | 통과 | rows 153, local_invalid 0, no_image 72, pending 81, 배치 [10×8, 1] |
| O-L-4 | 통과 | registered 81, api_calls 9/81/9, FRIEND 51 / NULL 1 / SSUM 29, 81\|81 고유, UPLOADED 81 1024/1536, S3 81 |
| O-L-5 | 통과 | already_in_ledger 81, api_calls 0/0/0 |
| O-L-6a | 통과 | conflict 2 (mediaId 163, 164 INITIATED). friend_001/002 poseId 82/83 은 그대로, tb_pose 81. admin_upload 3회(배치 1 + 개별 2). 서버 WARN `[POSE_SOURCE_KEY_CONFLICT] keys=[...]` |
| O-L-6b | 통과 | tb_pose 80 → ssum_028 CONFLICT(165), ssum_029 REGISTERED(media 166 UPLOADED) → 81 |
| O-L-6c | 통과 | 400 D-01 ×3, 403 D-995, 409 D-18. 고아 media 상태와 tb_pose 수 변화 없음 |
| O-L-6d | 통과 | 확인 쿼리로 163/164/165 INITIATED 확인, 가드 조건 DELETE 가 3건을 반환, S3 3건 삭제. 고아 0, POSE media 81, S3 81 |
| O-L-7 | 통과 | 51, 20/true, 9/false, 0, 0, 1(null), ASC 와 생략 결과가 같음(81건 내림차순), SSUM 첫 항목 ssum_029_M1-4_F1-1, 200 image/png |
| (추가) | 통과 | 연결 거부 서버로 실행하면 `aborted: 통신 예외 …`, 종료코드 2, 원장 저장, 재시도 없음. 토큰이 비면 종료코드 1 |
