# LLD : 알림 발송 공통 파이프라인

이 문서는 알림 발송 잡 3종(`weeklyReminderJob`, `weekendExploreJob`, `holidayExploreJob`)이 공유하는 tasklet 루프, 중복 방지, 톤·문구 규칙, 적재 테이블, 실행 계약을 다룹니다. 잡마다 다른 것(스케줄, 대상 조건, 변수, 문구)은 같은 폴더의 잡별 문서가 다루고, 경계와 결정은 `docs/hld/2026-10-06-notification-push-hld.md` 가 다룹니다. 코드와 이 문서가 어긋나면 코드가 정본이고 이 문서를 고칩니다.

## 1. tasklet 루프

```text
job/{WeeklyReminder,WeekendExplore,HolidayExplore}Job   잡마다 파일 하나 : Job 빈 + Step 빈 + @StepScope tasklet 빈
PushNotificationTaskletFactory.create                    공통 의존(이력, 발송 포트, hist)을 채워 tasklet 생성. 잡은 종류·날짜·Reader 만 넘김
PushNotificationTasklet.execute                          1회 = 대상 1건
  1. 버퍼가 비면 SendTargetReader.readPage(cursor, 100) 를 당김. 소진이면 FINISHED
  2. notification_log 에 (userId, type, businessDate) 있으면 filter (NotificationProcessor.decide -> Skip)
  3. 없으면 톤 배정 + 문구 렌더 -> PushNotificationSender.send -> notification_log 적재 -> SUCCESS 면 tb_notification_hist 적재
  4. CONTINUABLE. TaskletStep 이 트랜잭션을 커밋하고 다시 부름
```

- 커밋 단위 = execute() 1회 = 1건 : TaskletStep 이 호출마다 트랜잭션을 열고 닫음. 발송(FCM, 트랜잭션 밖 부수효과)과 적재(DB) 가 dual-write 라 단위가 크면 한 건의 적재 실패가 같은 트랜잭션의 이미 보낸 건들까지 롤백시키고, 재실행 때 그만큼 중복 발송됨. 1건이면 그 창이 "실패한 그 1건" 으로 줄어듦
- `PAGE_SIZE = 100` : DB 조회 단위. 커밋 단위와 다름
- 건수 : tasklet 이 `StepContribution` 으로 올림. read = 버퍼에서 꺼낸 대상, filter = `ALREADY_SENT`, write = log 를 적재한 건(FAILED 포함). `BATCH_STEP_EXECUTION` 의 READ_COUNT / FILTER_COUNT / WRITE_COUNT
- chunk 지향 Step 을 쓰지 않는 이유 : 커밋 단위가 1건이라 청크의 묶음 효과가 없고, Reader·Processor·Writer 와 @StepScope 빈 6개가 tasklet 하나로 접힘. search 잡과 같은 `job/` + `tasklet/` 모양 (HLD DEC-1)

Job, Step, tasklet 빈은 잡마다 파일 하나(`job/WeeklyReminderJob` 등)가 갖습니다 (search 의 `SearchIndexJob` 과 같은 모양). 공통 의존은 `PushNotificationTaskletFactory` 가 들고 있고, 잡마다 다른 것은 `SendTargetReader` 구현과 `NotificationType` 뿐입니다.

## 2. Reader 계약

```kotlin
fun interface SendTargetReader {
    /** afterUserId 보다 큰 user_id 의 동의자를 size 건 읽어 잡 조건으로 거른 결과. */
    fun readPage(afterUserId: Long, size: Int): SendTargetPage
}

data class SendTargetPage(val targets: List<SendTarget>, val nextCursor: Long?)
```

- 모든 구현은 `NotificationRepository.findPushAgreedAfter(afterUserId, size)` 로 시작합니다. 동의 필터(`push_agreed = true`)의 단일 출처이며 잡별 리더는 이 페이지를 거를 뿐입니다
- `nextCursor` 는 거르기 전 동의자 페이지의 마지막 `user_id` 입니다. 페이지가 `size` 보다 작으면 `null`(소진). **거른 뒤 `targets` 가 비어도 `nextCursor` 가 있으면 소진이 아닙니다.** WEEKLY 처럼 조건이 좁은 잡은 한 페이지가 통째로 걸러질 수 있고, 그때 빈 결과를 소진으로 보면 뒤 페이지의 대상을 놓칩니다
- `PushNotificationTasklet` 은 버퍼가 비고 소진이 아닌 동안 다음 페이지를 계속 당깁니다. 단일 쓰레드 전제이며 `@StepScope` 라 실행마다 새 인스턴스입니다

## 3. 판정

```kotlin
val alreadySent = notificationLogRepository.exists(target.userId, type, businessDate)
when (val decision = NotificationProcessor.decide(target, type, alreadySent, businessDate)) {
    is SendDecision.Send -> dispatch(target, decision.message)      // 4절
    is SendDecision.Skip -> contribution.incrementFilterCount(1)   // ALREADY_SENT
}
```

`NotificationProcessor.decide` 는 `domain/notification` 의 순수 함수입니다. 동의는 다시 보지 않고 중복만 판정한 뒤 톤을 배정하고 문구를 렌더합니다.

## 4. 발송과 적재 (`dispatch`)

| `PushNotificationSender.send` 결과 | 적재 | 다음 건 |
|---|---|---|
| 정상 반환 (messageId) | `notification_log` `SUCCESS` + `tb_notification_hist` 1행 | 계속 |
| `BusinessException(PUSH_SEND_FAILED)` (`FirebaseMessagingException`) | `notification_log` `FAILED` | 계속 |
| `BusinessException(PUSH_NOT_CONFIGURED)` (`FirebaseMessaging` 빈 없음) | 없음. 예외 전파 | step FAILED -> job FAILED -> 종료 코드 0 아님 |
| 그 밖의 예외 | 없음. 예외 전파 | 위와 같음 |

- `link` 는 `null` 입니다. 배치 알림에 딥링크가 생기면 `dispatch` 에 더합니다
- hist 적재는 기존 `NotificationService.recordSentPush(NotificationCommand.SendPush(userId, token, type.name, title, body, null))` 입니다. api 의 `SendPushUseCase` 와 같은 경로라 `type` 값도 같은 규칙(문자열)입니다
- log 와 hist 는 같은 트랜잭션(execute 1회)입니다. hist 가 실패하면 그 건의 log 도 롤백되고 잡은 실패합니다 (HLD DEC-5)
- 트랜잭션은 TaskletStep 이 엽니다. tasklet 과 리포지터리에 `@Transactional` 을 두지 않습니다

## 5. 중복 방지

- 키 : `(user_id, notification_type, business_date)`
- 1차 : tasklet 의 `exists` 조회. 커밋된 이력만 보임
- 최종 : `notification_log` 의 `uq_notification_log_user_type_date` unique 제약
- `fcm_result` 는 보지 않습니다. `FAILED` 로 적재된 유저도 같은 `businessDate` 에는 다시 보내지 않습니다
- 전달 보장은 at-least-once 입니다. 발송 뒤 적재 전에 프로세스가 죽은 1건은 재실행 때 다시 갑니다

## 6. 톤 배정과 문구 렌더

- 톤 : `MessageTone.entries[floorMod(floorMod(userId, 3) + floorMod(businessDate.toEpochDay(), 3), 3)]`. `0 -> INFORMATIVE`, `1 -> FRIENDLY`, `2 -> SUGGESTIVE`. 선언 순서가 유효하며, 같은 `(userId, businessDate)` 면 재실행해도 같은 톤. 두 항을 각각 mod 한 뒤 더하는 이유는 `userId` 가 `Long.MAX_VALUE` 근처일 때의 오버플로 때문
- 렌더 규칙 (순서대로)
  1. 배정된 톤의 템플릿에 필요 변수가 없으면 그대로. `variableApplied = false`, `actualTone = 배정 톤`
  2. 필요 변수가 있고 값이 비어 있지 않으면 치환. `variableApplied = true`
  3. 필요 변수의 값이 없거나 blank 면 그 종류의 폴백 톤 템플릿으로. `variableApplied = false`, `actualTone = 폴백 톤`. 폴백 톤 템플릿은 변수를 필요로 하지 않음
- 폴백 톤 : `WEEKLY_REMINDER -> INFORMATIVE`, `WEEKEND_EXPLORE -> INFORMATIVE`, `HOLIDAY_EXPLORE -> SUGGESTIVE`
- 변수 토큰 : `[최근 업로드 요일]` = `MessageVariable.RECENT_UPLOAD_DAY`, `[공휴일명]` = `MessageVariable.HOLIDAY_NAME`. 템플릿 안에서는 `{최근 업로드 요일}` 처럼 중괄호

문구 테이블은 잡별 문서에 있고, 코드는 `MessageRenderer` 의 두 단계 `when` 이라 종류·톤 조합이 빠지면 컴파일이 막습니다.

## 7. 적재 테이블

### `notification_log` (V35, `NotificationLog` 엔티티)

| 컬럼 | 타입 | 엔티티 |
|---|---|---|
| `id` | BIGINT IDENTITY | `Long?` |
| `user_id` | BIGINT | `userId` |
| `notification_type` | VARCHAR(32) | `@Enumerated(STRING) NotificationType` |
| `message_tone` | VARCHAR(16) | `@Enumerated(STRING) MessageTone` (`actualTone`) |
| `variable_applied` | BOOLEAN | `variableApplied` |
| `title` | VARCHAR(255) | `title` |
| `body` | VARCHAR(500) | `body` |
| `business_date` | DATE | `LocalDate` |
| `fcm_result` | VARCHAR(16) | `@Enumerated(STRING) PushSendStatus` |
| `sent_at` | TIMESTAMP(6) WITH TIME ZONE | `Instant` (적재 시점) |

`created_at`/`updated_at` 이 없어 `BaseTimeEntity` 를 상속하지 않습니다. 생성은 `NotificationLog.of(target, type, message, businessDate, sendStatus)` 하나입니다.

### `tb_notification_hist` (V22, 기존 `NotificationHist`)

`user_id`, `type = NotificationType.name`, `title`, `body`, `link = null`. `created_at` 은 auditing. `title` 은 VARCHAR(100) 이라 공휴일명이 긴 경우를 포함해도 템플릿이 그 안에 듭니다.

## 8. 실행 계약

```text
java -jar neki-batch.jar --spring.batch.job.name=weekendExploreJob businessDate=2026-10-06
```

- 잡 이름 : `weeklyReminderJob`, `weekendExploreJob`, `holidayExploreJob`. 없는 이름은 기동 실패
- `businessDate` : `YYYY-MM-DD`. 필수. Prefect 가 run 예약 시각(KST)에서 정함. 없으면 Reader 빈 생성에서 실패
- `RunIdIncrementer` : 같은 `businessDate` 로 다시 돌려도 새 JobInstance. 직전이 FAILED 여도 restart 가 아니라 처음부터. 중복 발송은 5절이 막음
- 종료 코드 : COMPLETED 0, 그 외 0 아님. Prefect 가 이 값으로 flow 결과를 정함
- 스캔 범위 (`bootstrap/ComponentScanConfig`) : 기존에 더해 `com.neki.domain.notification`, `com.neki.domain.photo.infra.persist`, `com.neki.config.firebase`
- 설정 (`application.yaml`) : `application-firebase.yaml` import. 공휴일 CSV 경로는 `neki.notification.holiday-csv` (기본 `holidays.csv`. 테스트만 `holidays-test.csv` 로 바꿈)
- Firebase : staging/prod 는 `file:/etc/firebase/firebase-service-account.json`. batch Job 파드는 Secret `prefect-workflow` 의 `firebase-service-account.json` 키를 그 경로에 마운트 (Workflow 의 Job 매니페스트)

로컬 실행 (api 가 먼저 V35 를 적용해 둔 뒤) :

```bash
./gradlew :apps:batch:bootRun --args="--spring.batch.job.name=weekendExploreJob businessDate=2026-10-06"
```

로컬 프로파일은 `classpath:firebase-service-account.json` 을 찾습니다. 파일이 없으면 첫 발송에서 `PUSH_NOT_CONFIGURED` 로 실패하는 것이 의도된 동작입니다.

## 9. 코드 위치

| 관심사 | 위치 |
|---|---|
| 잡 (Job·Step·tasklet 빈) | `apps/batch/src/main/kotlin/com/neki/batch/notification/job/{WeeklyReminder,WeekendExplore,HolidayExplore}Job.kt` |
| tasklet | `apps/batch/.../notification/tasklet/PushNotificationTasklet.kt`, `PushNotificationTaskletFactory.kt` |
| Reader 계약 | `apps/batch/.../notification/tasklet/SendTargetReader.kt` |
| 잡별 Reader | `apps/batch/.../notification/tasklet/{WeeklyReminder,WeekendExplore,HolidayExplore}TargetReader.kt` |
| 공휴일 | `domain/.../notification/models/Holiday.kt`, `repository/HolidayRepository.kt`, `infra/csv/CsvHolidayRepositoryAdapter.kt`, `domain/src/main/resources/holidays.csv` |
| 도메인 모델·정책 | `domain/src/main/kotlin/com/neki/domain/notification/{models/*, MessageRenderer, ToneAssignmentPolicy, NotificationProcessor}.kt` |
| 발송 이력 | `domain/.../notification/models/NotificationLog.kt`, `repository/NotificationLogRepository.kt`, `infra/persist/NotificationLogRepositoryAdapter.kt` |
| 동의자 페이지 | `domain/.../notification/repository/NotificationRepository.kt` `findPushAgreedAfter` |
| 업로드 집계 | `domain/.../photo/repository/PhotoImageRepository.kt` `findUserIdsUploadedBetween`, `findLastUploadedAtByUserIds` |
| 스키마 | `modules/postgres/src/main/resources/db/migration/V35__create_notification_log_table.sql` |

## 10. 테스트

- 순수 규칙 : `domain/src/test/kotlin/com/neki/domain/notification/{MessageRendererTest, ToneAssignmentPolicyTest, NotificationProcessorTest, EnumContractTest}.kt`
- 잡 E2E (H2) : `apps/batch/src/test/kotlin/com/neki/batch/notification/job/NotificationPushJobsTest.kt`. stub `PushNotificationSender` 로 발송을 기록하고 log/hist 를 단언
- 공휴일 CSV : `domain/src/test/kotlin/com/neki/domain/notification/infra/csv/CsvHolidayRepositoryAdapterTest.kt`
