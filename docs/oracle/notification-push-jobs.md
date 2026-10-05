# 오라클 : 알림 발송 배치 이관 (BACKEND-135)

이 문서는 BACKEND-135 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 각 저장소 루트에서 그대로 실행합니다. 계획은 `docs/superpowers/plans/2026-10-06-notification-batch-migration.md`, 설계는 `docs/hld/2026-10-06-notification-push-hld.md` 입니다.

## 공통 (O-0, Server)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (T0 는 계획의 baseline 절) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc) $(grep -ho 'failures="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | batch bootJar 빌드 성공 | `./gradlew :apps:batch:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.sql' '*.yaml' '*.yml' '*.csv' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-5 | auto | jOOQ 가 들어오지 않음 | `grep -rn "org.jooq" --include=*.kt --include=*.kts . \| grep -v "^./docs" \| wc -l` = 0 |
| O-0-6 | auto | 도메인 간 import 없음 (notification 이 photo 를, photo 가 notification 을 모름) | `grep -rln "com.neki.domain.photo" domain/src/main/kotlin/com/neki/domain/notification \| wc -l` = 0, `grep -rln "com.neki.domain.notification" domain/src/main/kotlin/com/neki/domain/photo \| wc -l` = 0 |
| O-0-7 | auto | batch 에 `@Transactional` 없음 (TaskletStep 이 트랜잭션을 연다) | `grep -rc "@Transactional" apps/batch/src/main \| grep -v ":0" \| wc -l` = 0 |

## O-A. 스키마, 엔티티, 포트

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | `test -f modules/postgres/src/main/resources/db/migration/V35__create_notification_log_table.sql` 종료코드 0, V35 보다 큰 번호 없음 : `ls modules/postgres/src/main/resources/db/migration \| grep -cE "^V3[6-9]__\|^V[4-9][0-9]__"` = 0 |
| O-A-2 | auto | 편입 방식 : `grep -c "CREATE TABLE IF NOT EXISTS notification_log" modules/postgres/src/main/resources/db/migration/V35__create_notification_log_table.sql` = 1, `grep -c "uq_notification_log_user_type_date" ...V35...sql` = 1 |
| O-A-3 | auto | 엔티티 컬럼명 전부 snake_case : `grep -ohE 'name = "[^"]+"' domain/src/main/kotlin/com/neki/domain/notification/models/NotificationLog.kt \| grep -c "[A-Z]"` = 0 |
| O-A-4 | auto | 엔티티에 `unique = true` 없고 `@Table(uniqueConstraints` 로 선언 : `grep -c "unique = true" domain/src/main/kotlin/com/neki/domain/notification/models/NotificationLog.kt` = 0, `grep -c "uniqueConstraints" 같은파일` = 1 |
| O-A-5 | auto | 포트 메서드 : `grep -c "fun findPushAgreedAfter" domain/src/main/kotlin/com/neki/domain/notification/repository/NotificationRepository.kt` = 1, `grep -cE "fun findUserIdsUploadedBetween\|fun findLastUploadedAtByUserIds" domain/src/main/kotlin/com/neki/domain/photo/repository/PhotoImageRepository.kt` = 2, `grep -cE "fun exists\|fun save" domain/src/main/kotlin/com/neki/domain/notification/repository/NotificationLogRepository.kt` = 2, `grep -c "fun findByNotifyDate" domain/src/main/kotlin/com/neki/domain/notification/repository/HolidayRepository.kt` = 1 |
| O-A-6 | auto | 도메인 모델·정책 위치 : `ls domain/src/main/kotlin/com/neki/domain/notification/models/{NotificationType,MessageTone,MessageVariable,RenderedMessage,SendTarget,SendDecision,PushSendStatus,Holiday,NotificationLog}.kt domain/src/main/kotlin/com/neki/domain/notification/{MessageRenderer,ToneAssignmentPolicy,NotificationProcessor}.kt` 종료코드 0 |
| O-A-7 | auto | api 가 H2 에 `notification_log` DDL 을 만들고 기동함 : `./gradlew :apps:api:test --tests 'com.neki.api.rule.ArchitectureRulesTest' -q` 종료코드 0 |

## O-B. 도메인 순수 규칙

| id | 종류 | 판정 |
|---|---|---|
| O-B-1 | auto | `./gradlew :domain:test --tests 'com.neki.domain.notification.*' -q` 종료코드 0 |
| O-B-2 | auto | 문구 9개 조합이 코드에 verbatim : `grep -c "일주일 전 사진이 있어요\|벌써 일주일 전 네컷이에요\|처럼 오늘도 남겨볼까요?\|주말 전 포토부스 확인하기\|이번 주말엔 어디서 찍을까요?\|약속 전에 미리 찾아보세요\|{공휴일명} 포토부스 확인하기\|{공휴일명}에 약속 있으신가요?\|쉬는 날 가기 좋은 포토부스" domain/src/main/kotlin/com/neki/domain/notification/MessageRenderer.kt` = 9 |
| O-B-3 | auto | 톤 배정이 오버플로 안전 : `grep -c "floorMod" domain/src/main/kotlin/com/neki/domain/notification/ToneAssignmentPolicy.kt` >= 3 |
| O-B-4 | auto | domain 테스트 수가 baseline + 4 파일 분만큼 늘어남 : `./gradlew :domain:test -q; grep -ho 'tests="[0-9]*"' domain/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` >= baseline + 30 |

## O-C. 배치 잡

| id | 종류 | 판정 |
|---|---|---|
| O-C-1 | auto | `./gradlew :apps:batch:test -q` 종료코드 0, batch 테스트 수 >= baseline + 10 |
| O-C-2 | auto | 잡 3개와 `RunIdIncrementer` : `grep -cE "WEEKLY_REMINDER_JOB\|WEEKEND_EXPLORE_JOB\|HOLIDAY_EXPLORE_JOB" apps/batch/src/main/kotlin/com/neki/batch/notification/job/NotificationPushJobConfig.kt` >= 3, `grep -c "RunIdIncrementer()" 같은파일` = 1 (헬퍼 한 곳) |
| O-C-3 | auto | 커밋 단위 1건 (execute 1회 = 1건) : `grep -c "RepeatStatus.CONTINUABLE" apps/batch/src/main/kotlin/com/neki/batch/notification/tasklet/PushNotificationTasklet.kt` = 1, `grep -c "incrementReadCount" 같은파일` = 1 |
| O-C-4 | auto | Reader 가 동의 필터의 단일 출처를 거침 : `grep -rc "findPushAgreedAfter(" apps/batch/src/main/kotlin/com/neki/batch/notification/tasklet \| grep -v ":0" \| wc -l` = 3 |
| O-C-5 | auto | 커서가 거르기 전 페이지 기준 : `grep -c "nextCursor" apps/batch/src/main/kotlin/com/neki/batch/notification/tasklet/PushNotificationTasklet.kt` >= 1 |
| O-C-6 | auto | tasklet 이 미설정을 삼키지 않음 : `grep -c "PUSH_SEND_FAILED" apps/batch/src/main/kotlin/com/neki/batch/notification/tasklet/PushNotificationTasklet.kt` >= 1, `grep -c "PUSH_NOT_CONFIGURED" 같은파일` = 0 (분기 없이 전파) 또는 rethrow |
| O-C-7 | auto | hist 는 기존 서비스 경로 : `grep -c "recordSentPush" apps/batch/src/main/kotlin/com/neki/batch/notification/tasklet/PushNotificationTasklet.kt` = 1 |
| O-C-8 | auto | 스캔 범위와 설정 : `grep -cE '"com.neki.domain.notification"\|"com.neki.domain.photo.infra.persist"\|"com.neki.config.firebase"' apps/batch/src/main/kotlin/com/neki/batch/bootstrap/ComponentScanConfig.kt` = 3, `grep -c "application-firebase.yaml" apps/batch/src/main/resources/application.yaml` = 1, `grep -c 'project(":modules:firebase")' apps/batch/build.gradle.kts` = 1 |
| O-C-9 | auto | 공휴일 CSV 번들 : `test -f domain/src/main/resources/holidays.csv` 종료코드 0, `grep -c "^2026-" domain/src/main/resources/holidays.csv` >= 10 |
| O-C-10 | auto | 처리량 상한 표시 : `grep -rc "ponytail:" apps/batch/src/main/kotlin/com/neki/batch/notification \| grep -v ":0" \| wc -l` >= 1 |
| O-C-11 | auto | 문서에 잡 이름 : `grep -c "weekendExploreJob" README.md .claude/CLAUDE.md` 두 파일 모두 >= 1 |
| O-C-12 | auto | 기존 잡 회귀 : `./gradlew :apps:batch:test --tests 'com.neki.batch.search.job.SearchIndexJobTest' --tests 'com.neki.batch.ExitCodeTest' -q` 종료코드 0 |

## O-D. Workflow (Team-Neki-Workflow 루트)

| id | 종류 | 판정 |
|---|---|---|
| O-D-1 | auto | `make check` 종료코드 0 (임포트, deployment 수집, spec anchor, pytest) |
| O-D-2 | auto | deployment 3개 : `uv run python -c "from deployments import collect; print(sorted(d.flow_name + '/' + d.name for d in collect()))"` 출력에 `holiday-explore/holiday-explore`, `weekend-explore/weekend-explore`, `weekly-reminder/weekly-reminder` 포함 |
| O-D-3 | auto | cron 과 타임존 : `grep -cE 'Cron\("0 20 \* \* \*"' deployments/weekly_reminder.py` = 1, `grep -cE 'Cron\("0 18 \* \* 5,6,0"' deployments/weekend_explore.py` = 1, `grep -cE 'Cron\("0 9 \* \* \*"' deployments/holiday_explore.py` = 1, `grep -c 'timezone="Asia/Seoul"' deployments/{weekly_reminder,weekend_explore,holiday_explore}.py` 세 파일 모두 1 |
| O-D-4 | auto | 직렬 실행 : `grep -c "concurrency_limit=1" deployments/{weekly_reminder,weekend_explore,holiday_explore}.py` 세 파일 모두 1 |
| O-D-5 | auto | Job 매니페스트에 Firebase 마운트 : `uv run pytest -q tests/test_notification_push.py` 종료코드 0 |
| O-D-6 | auto | search-index 회귀 : `uv run pytest -q tests/test_search_index.py tests/test_search_index_wait.py` 종료코드 0 |
| O-D-7 | auto | spec 문서 : `test -f docs/spec/notification-push.md` 종료코드 0, `make spec-check` 종료코드 0 |

## O-E. GitOps (Team-Neki-GitOps 루트)

| id | 종류 | 판정 |
|---|---|---|
| O-E-1 | auto | Secret 예시에 키 : `grep -cE "firebase-service-account.json\|SPRING_PROFILES_ACTIVE\|JASYPT_PASSWORD" overlays/prefect/workflow-secret.example.yaml` >= 3 |
| O-E-2 | auto | Notification Deployment 제거 : `test ! -e overlays/prod/notification-deployment.yaml` 종료코드 0, `grep -c "notification-deployment.yaml" overlays/prod/kustomization.yaml` = 0 |
| O-E-3 | auto | kustomize 빌드 : `kubectl kustomize overlays/prod \| grep -c "neki-notification"` = 0 |

## O-R. 릴리스와 전환 (HLD 11절 순서)

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | Server PR : `gh pr view feat/BACKEND-135 --json state,baseRefName --jq '.state + " " + .baseRefName'` = `OPEN main` (머지 전), CI `gh pr checks feat/BACKEND-135` 전부 SUCCESS |
| O-R-2 | auto | Workflow PR, GitOps PR 이 각각 OPEN (`gh pr list --head feat/BACKEND-135` 각 저장소) |
| O-R-3 | manual | Prefect 환경 확인 : 노드에서 `kubectl -n prefect get secret prefect-workflow -o jsonpath='{.data.SPRING_PROFILES_ACTIVE}' \| base64 -d` 가 `prod`. 아니면 4단계 이후 중단 |
| O-R-4 | manual | api 배포 뒤 prod `select count(*) from notification_log` 가 배포 전과 같음(데이터 인계), staging 은 테이블 존재 |
| O-R-5 | manual | `deploy-batch.yml` 실행 뒤 GitOps `images.env` 갱신, Prefect UI 에 deployment 3개가 paused 로 보임 |
| O-R-6 | manual | `prefect deployment run weekend-explore/weekend-explore` -> k8s Job 종료 코드 0, `notification_log` 에 오늘 `WEEKEND_EXPLORE` 행, `tb_notification_hist` 에 같은 유저 행, 수신 기기에 알림 표시·탭 동작 종전과 같음 |
| O-R-7 | manual | 같은 날 한 번 더 실행 -> 발송 0, `BATCH_JOB_INSTANCE` 인스턴스 +1 |
| O-R-8 | manual | GitOps PR 머지 -> `kubectl -n prod get deploy neki-notification` 없음 -> deployment 3개 unpause -> 다음 cron 에 flow run 생성 |
| O-R-9 | manual | Notification 저장소 아카이브, 티켓 BACKEND-135 DONE |
