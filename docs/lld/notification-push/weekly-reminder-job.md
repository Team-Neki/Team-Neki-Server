# LLD : weeklyReminderJob (아카이빙 1주일 리마인드)

이 문서는 `WEEKLY_REMINDER` 발송 잡의 동작을 다룹니다. 공통 파이프라인(청크, 중복 방지, 톤·폴백 규칙, 실행 계약)은 `pipeline.md` 에 있고 여기서는 이 잡만의 것을 적습니다.

## 목적

7일 전에 사진을 올린 사용자에게 그 사진을 다시 보도록 매일 저녁 알립니다.

## 스케줄과 실행

- Prefect deployment : `weekly-reminder/weekly-reminder`, cron `0 20 * * *` (Asia/Seoul), `concurrency_limit=1`
- 잡 : `--spring.batch.job.name=weeklyReminderJob businessDate=<D>`
- `businessDate` 는 발송일 D. 업로드 기준일은 `D - 7`

## 대상 조건

`push_agreed = true` 이고 `D - 7` 당일(00:00 이상 익일 00:00 미만, KST 벽시계)에 삭제되지 않은 사진을 올린 사용자.

```text
1. NotificationRepository.findPushAgreedAfter(cursor, 100)             동의자 페이지 (user_id 오름차순)
2. PhotoImageRepository.findUserIdsUploadedBetween(ids, D-7 00:00, D-6 00:00)
                                                                        그 페이지에서 D-7 당일 업로드가 있는 user_id 집합
3. PhotoImageRepository.findLastUploadedAtByUserIds(남은 ids)           user_id -> MAX(created_at) (삭제 제외)
4. 남은 유저마다 SendTarget(userId, deviceToken, {RECENT_UPLOAD_DAY: "지난 <요일>"})
```

등가 SQL (설명용. 실제는 위 포트 3개를 조합) :

```sql
select n.user_id, n.device_token,
       (select max(p.created_at) from tb_photo_image p where p.user_id = n.user_id and p.deleted_at is null) as last_upload
from tb_notification n
where n.push_agreed = true
  and n.user_id > :cursor
  and exists (select 1 from tb_photo_image p
              where p.user_id = n.user_id and p.deleted_at is null
                and p.created_at >= :d_minus_7 and p.created_at < :d_minus_6)
order by n.user_id
limit 100
```

- soft-delete 제외는 `PhotoImage` 의 `@SQLRestriction("deleted_at IS NULL")` 이 두 포트 모두에 적용합니다. 지운 사진을 근거로 알림이 가지 않습니다
- `created_at` 은 시간대 없는 `TIMESTAMP` 에 KST 벽시계입니다. 경계는 `LocalDateTime` 으로 비교합니다

## 변수

`[최근 업로드 요일]` = `"지난 " + MAX(created_at).dayOfWeek` 의 한국어 요일 (e.g. `지난 토요일`). 삭제되지 않은 사진 중 가장 최근 업로드가 기준이라, D-7 뒤에 또 올렸으면 그 요일이 들어갑니다. 값이 없을 수는 없지만(2단계를 통과한 유저는 업로드가 있음) 혹시 비면 폴백 톤으로 내려갑니다.

## 문구

| 톤 | 제목 | 본문 | 변수 |
|---|---|---|---|
| INFORMATIVE (폴백) | 일주일 전 사진이 있어요 | 네키에 저장한 네컷을 다시 확인해보세요. | 없음 |
| FRIENDLY | 벌써 일주일 전 네컷이에요 | 지난 사진을 네키에서 다시 꺼내보세요. | 없음 |
| SUGGESTIVE | {최근 업로드 요일}처럼 오늘도 남겨볼까요? | 오늘 찍은 사진도 네키에 정리해보세요. | `[최근 업로드 요일]` |

## 엣지

- D-7 에 올린 사진을 전부 지움 : 2단계에서 빠짐. 발송 없음
- 한 페이지(동의자 100명) 중 D-7 업로드자가 없음 : `targets` 는 비고 `nextCursor` 는 있음. 다음 페이지로 이어감
- 같은 날 두 번 실행 : 2회차는 전원 `ALREADY_SENT`
- 업로드가 많은 유저 : `findLastUploadedAtByUserIds` 가 유저당 1행만 돌려 변수는 하나

## 코드

- `apps/batch/.../notification/reader/WeeklyReminderTargetReader.kt` (조건·변수), `KoreanWeekday` (요일 표기)
- Job 빈 `weeklyReminderJob`, Step `weeklyReminderStep` : `NotificationPushJobConfig`

## 수동 실행과 확인

```bash
prefect deployment run weekly-reminder/weekly-reminder --param target_date=2026-10-06
```

```sql
select user_id, message_tone, variable_applied, title, fcm_result
from notification_log
where notification_type = 'WEEKLY_REMINDER' and business_date = '2026-10-06'
order by user_id;
```

## 테스트

`NotificationPushJobsTest` : D-7 업로드 동의자만 발송, SUGGESTIVE 톤의 `지난 목요일처럼 오늘도 남겨볼까요?` 치환과 `variable_applied = true`, soft-delete 제외, 첫 페이지가 전부 걸러져도 둘째 페이지의 대상이 발송됨.
