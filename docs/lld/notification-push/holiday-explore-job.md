# LLD : holidayExploreJob (연휴·공휴일 탐색)

이 문서는 `HOLIDAY_EXPLORE` 발송 잡의 동작을 다룹니다. 공통 파이프라인은 `pipeline.md` 에 있고 여기서는 이 잡만의 것을 적습니다.

## 목적

공휴일의 발송일에 최근 한 달 안에 사진을 올린 사용자에게 쉬는 날 갈 포토부스를 찾아보도록 알립니다.

## 스케줄과 실행

- Prefect deployment : `holiday-explore/holiday-explore`, cron `0 9 * * *` (Asia/Seoul), `concurrency_limit=1`
- 잡 : `--spring.batch.job.name=holidayExploreJob businessDate=<D>`
- 매일 돌되 D 가 어떤 공휴일의 발송일일 때만 보냅니다. 아니면 0건으로 COMPLETED

## 발송일 판정

`domain/src/main/resources/holidays.csv` 가 원천입니다.

```text
# holiday_date,name,notify_offset_days   (ISO 날짜, UTF-8)
2026-10-09,한글날,0
```

- 발송일 = `holiday_date + notify_offset_days` (전날 -1, 당일 0). 생략하면 0
- 주석(`#`), 빈 줄, 헤더 행 허용. `name` 에 쉼표 불가
- 연휴는 날짜마다 한 행. 대체공휴일은 원 공휴일 이름으로 (e.g. 2026-08-17 은 `광복절`)
- `HolidayRepository.findByNotifyDate(D)` 가 발송일이 D 인 행을 돌려줍니다. 둘 이상이면 첫 행
- `notify_offset_days` 의 절대값이 2 를 넘으면 데이터 오류일 가능성이 커 WARN 을 남기되 판정은 막지 않습니다
- CSV 는 domain 리소스라 빈 생성 시 한 번 읽습니다. 갱신은 배포입니다 (HLD DEC-7)

## 대상 조건

발송일일 때, `push_agreed = true` 이고 `D - 1개월` 00:00 이후(KST 벽시계)에 삭제되지 않은 사진을 올린 사용자.

```text
0. HolidayRepository.findByNotifyDate(D) 가 null 이면 빈 페이지 (nextCursor = null)
1. NotificationRepository.findPushAgreedAfter(cursor, 100)        동의자 페이지
2. PhotoImageRepository.findLastUploadedAtByUserIds(ids)           user_id -> MAX(created_at) (삭제 제외)
3. MAX(created_at) >= D-1개월 00:00 인 유저만
4. SendTarget(userId, deviceToken, {HOLIDAY_NAME: <공휴일명>})      변수는 실행 단위 값이라 전원 동일
```

등가 SQL :

```sql
select n.user_id, n.device_token
from tb_notification n
where n.push_agreed = true and n.user_id > :cursor
  and exists (select 1 from tb_photo_image p
              where p.user_id = n.user_id and p.deleted_at is null
                and p.created_at >= :d_minus_1_month)
order by n.user_id
limit 100
```

"최근 1달 지도 사용 이력" 조건은 데이터 원천이 없어(Server 이슈 #292) 업로드 이력만 봅니다. 종전과 같습니다.

## 변수

`[공휴일명]` = CSV 의 `name`. 발송 단위 값이라 그날 대상 전원이 같은 값을 받습니다.

## 문구

| 톤 | 제목 | 본문 | 변수 |
|---|---|---|---|
| INFORMATIVE | {공휴일명} 포토부스 확인하기 | 쉬는 날 방문할 포토부스를 네키 지도에서 확인해보세요. | `[공휴일명]` |
| FRIENDLY | {공휴일명}에 약속 있으신가요? | 약속 전에 근처 포토부스를 미리 확인해보세요! | `[공휴일명]` |
| SUGGESTIVE (폴백) | 쉬는 날 가기 좋은 포토부스 | 네키 지도에서 가까운 포토부스를 확인해보세요. | 없음 |

## 엣지

- 발송일 아님 : Reader 가 처음부터 소진. read 0, write 0, COMPLETED. 매일 이 경로가 대부분
- 연휴 3일 : 날짜마다 행이 있으면 사흘 연속 각각 발송. `businessDate` 가 달라 중복 방지 키도 다름
- 같은 날 두 번 실행 : 2회차는 전원 `ALREADY_SENT`
- CSV 가 없거나 형식 오류 : `CsvHolidayRepositoryAdapter` 생성에서 실패 -> 잡 FAILED. 조용히 0건으로 끝나지 않음

## 코드

- `apps/batch/.../notification/step/HolidayExploreTargetReader.kt`
- `domain/.../notification/models/Holiday.kt`, `repository/HolidayRepository.kt`, `infra/csv/CsvHolidayRepositoryAdapter.kt`, `domain/src/main/resources/holidays.csv`
- Job 빈 `holidayExploreJob`, Step `holidayExploreStep` : `NotificationPushJobConfig`

## 수동 실행과 확인

```bash
prefect deployment run holiday-explore/holiday-explore --param target_date=2026-10-09
```

```sql
select user_id, message_tone, title, fcm_result
from notification_log
where notification_type = 'HOLIDAY_EXPLORE' and business_date = '2026-10-09'
order by user_id;
```

## 테스트

- `CsvHolidayRepositoryAdapterTest` : 주석·빈 줄·헤더·offset 생략 파싱, `notify_offset_days` 가 발송일에 반영됨
- `NotificationPushJobsTest` : 발송일이 아니면 0건 COMPLETED, 발송일이면 최근 1달 업로드 동의자에게 FRIENDLY 톤의 `테스트공휴일에 약속 있으신가요?` 치환 (테스트 CSV `holidays-test.csv`)
