# LLD : weekendExploreJob (주말 전 탐색)

이 문서는 `WEEKEND_EXPLORE` 발송 잡의 동작을 다룹니다. 공통 파이프라인은 `pipeline.md` 에 있고 여기서는 이 잡만의 것을 적습니다.

## 목적

주말을 앞둔 금·토·일 저녁에 푸시 동의자 전원에게 근처 포토부스를 찾아보도록 알립니다.

## 스케줄과 실행

- Prefect deployment : `weekend-explore/weekend-explore`, cron `0 18 * * 5,6,0` (Asia/Seoul), `concurrency_limit=1`
- 잡 : `--spring.batch.job.name=weekendExploreJob businessDate=<D>`
- `businessDate` 는 중복 방지 키와 톤 배정에만 쓰입니다. 대상 조건에는 쓰이지 않습니다

## 대상 조건

`push_agreed = true` 전원.

```text
1. NotificationRepository.findPushAgreedAfter(cursor, 100)   동의자 페이지
2. 그대로 SendTarget(userId, deviceToken)                    거르지 않음, 변수 없음
```

등가 SQL :

```sql
select n.user_id, n.device_token
from tb_notification n
where n.push_agreed = true and n.user_id > :cursor
order by n.user_id
limit 100
```

## 변수

없습니다. 변수 맵이 채워져 들어와도 이 종류의 템플릿은 변수를 쓰지 않습니다.

## 문구

| 톤 | 제목 | 본문 |
|---|---|---|
| INFORMATIVE (폴백) | 주말 전 포토부스 확인하기 | 가까운 포토부스를 네키 지도에서 확인해보세요. |
| FRIENDLY | 이번 주말엔 어디서 찍을까요? | 약속 전에 근처 포토부스를 미리 찾아보세요. |
| SUGGESTIVE | 약속 전에 미리 찾아보세요 | 가까운 포토부스를 네키 지도에서 확인해보세요. |

## 엣지

- 세 잡 중 대상이 가장 많습니다. 발송 시간은 동의자 수에 비례하며 (건당 FCM 왕복 + 커밋) Prefect flow 타임아웃 3,600초 안에 끝나야 합니다. 넘기면 `FirebaseMessaging.sendEach` 로 묶는 것이 다음 단계입니다
- 같은 날 두 번 실행 : 2회차는 전원 `ALREADY_SENT`
- 금·토·일은 `businessDate` 가 달라 각각 따로 갑니다. 한 유저가 주말 동안 최대 3번 받습니다 (종전과 같음)

## 코드

- `apps/batch/.../notification/step/WeekendExploreTargetReader.kt`
- Job 빈 `weekendExploreJob`, Step `weekendExploreStep` : `NotificationPushJobConfig`

## 수동 실행과 확인

```bash
prefect deployment run weekend-explore/weekend-explore --param target_date=2026-10-06
```

```sql
select fcm_result, count(*)
from notification_log
where notification_type = 'WEEKEND_EXPLORE' and business_date = '2026-10-06'
group by 1;
```

전환 검증(HLD 11절 5단계)은 이 잡으로 합니다. 조건이 없어 결과를 해석하기 가장 쉽습니다.

## 테스트

`NotificationPushJobsTest` : 동의자 전원 발송·log·hist 적재와 미동의 제외, 같은 `businessDate` 재실행 시 0건 발송과 새 JobInstance, `PUSH_SEND_FAILED` 1건은 `FAILED` 로 남고 나머지는 계속, `PUSH_NOT_CONFIGURED` 는 잡 FAILED.
