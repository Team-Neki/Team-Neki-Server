# 오라클 : 클라이언트 행동 이벤트 수집 API (BACKEND-166)

이 문서는 BACKEND-166 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 staging 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다. staging 요청은 `http/client-log.http` 에 있고, 각 요청의 `client.test` 가 판정을 대신합니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (domain 32, apps/batch 18, apps/api 585) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc) $(grep -ho 'failures="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | api bootJar 빌드 성공 | `./gradlew :apps:api:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.yaml' '*.http' '*.json' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-5 | auto | 토큰 파일이 커밋되지 않음 | `git ls-files http/http-client.private.env.json \| wc -l` = 0, `git check-ignore -q http/http-client.private.env.json` 종료코드 0 |

## O-A. 코드와 설정

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | 엔드포인트·어댑터 테스트(NDJSON 묶음·분할 포함)와 아키텍처 규칙 통과 : `./gradlew :apps:api:test --tests '*CollectClientLogsE2ETest' --tests '*FirehoseClientLogAdapterTest' --tests '*ArchitectureRulesTest' -q` 종료코드 0 |
| O-A-2 | auto | staging·prod 스트림 이름이 Platform ADR-0004 와 같음 : `grep -cE "delivery-stream: team-neki-log-raw-(staging\|production)-client-log$" modules/aws/src/main/resources/application-s3.yaml` = 2 |
| O-A-3 | auto | 실제 어댑터는 staging·prod 에서만 뜸 : `grep -l '@Profile("!test & !local")' modules/aws/src/main/kotlin/com/neki/config/aws/FirehoseConfig.kt domain/src/main/kotlin/com/neki/domain/support/infra/firehose/FirehoseClientLogAdapter.kt \| wc -l` = 2 |
| O-A-4 | auto | fake 는 test·local 에서만 뜸 : `grep -c '@Profile("test \| local")' domain/src/main/kotlin/com/neki/domain/support/infra/firehose/fake/FakeClientLogSender.kt` = 1 |
| O-A-5 | auto | 결과 코드 D-15 와 D-16 이 각각 하나뿐 : `grep -Ec '"D-1[56]"' core/src/main/kotlin/com/neki/core/code/ResultCode.kt` = 2 |
| O-A-6 | auto | `/api/logs` POST 만 익명 허용, 잘못된 Authorization 은 거절 : `CollectClientLogsE2ETest` 의 익명·잘못된 헤더 테스트 통과 |
| O-A-7 | auto | support 도메인 밖으로 새지 않음 (새 도메인 패키지 없음) : `test ! -e domain/src/main/kotlin/com/neki/domain/clientlog && test ! -e apps/api/src/main/kotlin/com/neki/api/clientlog` 종료코드 0 |

## O-D. staging 배포 후 검증

staging 전송 스트림은 이미 생성되어 있습니다. 배포 후 API 응답과 S3 적재를 확인합니다. 스트림 미생성 상태를 전제로 한 O-D-5 는 과거 계획이므로 해당 없음으로 기록하고, D-15 응답은 자동 테스트로 확인합니다.

### 준비

- 배포 : `gh workflow run deploy-api-staging.yml --ref feat/BACKEND-166` (staging 은 수동 dispatch 만 가능)
- 토큰 : `http/http-client.private.env.json` 에 `{ "staging": { "accessToken": "..." } }` 작성. staging 앱 로그인 응답이나 `POST /api/auth/refresh` 응답의 `data.accessToken` 사용
- 실행 : IntelliJ HTTP Client 에서 환경 `staging` 선택 후 요청별 실행. CLI 는 `ijhttp --env-file http/http-client.env.json --private-env-file http/http-client.private.env.json --env staging http/client-log.http`
- S3 확인 변수 : `BUCKET=team-neki-log-staging`, `PREFIX=raw/client-log/` (Platform ADR-0004. 운영은 `team-neki-log-production` 의 같은 경로)
- 전제 : Team-Neki-Platform#23 에서 생성한 staging 스트림이 `ACTIVE`

### API 응답

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-0 | manual | staging 이미지가 브랜치 HEAD 로 갱신됨 | Team-Neki-GitOps 에서 `grep -o 'yapp-dev:[^ ]*' overlays/staging/deployment.yaml` 의 접미사 = `git rev-parse --short=7 feat/BACKEND-166`, ArgoCD `staging` 앱 Synced·Healthy |
| O-D-1 | manual | OpenAPI 문서에 `/api/logs` 노출 | `http/client-log.http` 의 O-D-1 통과 |
| O-D-2 | manual | 토큰 유효, userId 기록 | O-D-2 통과. 로그에 찍힌 `userId` 를 O-D-7 에서 사용 |
| O-D-3 | manual | 잘못된 Authorization 은 403 D-998 | O-D-3 통과 |
| O-D-4 | manual | 빈 배치는 400 D-01 | O-D-4 통과 |
| O-D-5 | manual | 해당 없음. 스트림 생성 전 실패 응답을 확인하려던 과거 계획 | 이미 staging 스트림이 생성됨. 503 D-15 는 `ExceptionHandlerTest` 와 어댑터 실패 테스트로 확인 |

### Firehose·S3 적재

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-D-S | manual | staging 스트림 ACTIVE, 목적지가 staging 버킷의 `raw/client-log/`, GZIP | `aws firehose describe-delivery-stream --delivery-stream-name team-neki-log-raw-staging-client-log --query 'DeliveryStreamDescription.[DeliveryStreamStatus,Destinations[0].ExtendedS3DestinationDescription.[BucketARN,Prefix,CompressionFormat,BufferingHints.IntervalInSeconds]]'` = `ACTIVE`, `arn:aws:s3:::team-neki-log-staging`, `raw/client-log/year=...`, `GZIP` |
| O-D-6 | manual | 2건 전송 성공 D-0 | O-D-6 통과 |
| O-D-7 | manual | 버퍼 시간 경과 후 인증 이벤트 2줄 적재. 인증 ID와 발생 당시 보고 ID, 세션·발생 시각·속성이 각각 유지됨 | `KEY=$(aws s3 ls s3://$BUCKET/$PREFIX --recursive \| sort \| tail -1 \| awk '{print $4}')` 뒤 `aws s3 cp s3://$BUCKET/$KEY - \| gunzip \| grep '"eventType":"oracle_authenticated"' \| jq -c '{userId, platform, receivedAt, eventId: .log.eventId, occurredAt: .log.occurredAt, sessionId: .log.sessionId, sequenceNumber: .log.sequenceNumber, deviceId: .log.deviceId, appVersion: .log.appVersion, seq: .log.eventProperties.seq, reportedUserId: .log.userIdAtOccurrence}'` 에서 seq 1, 2 두 줄. 두 줄 모두 `userId` = O-D-2 값, `platform` = `IOS`, `sessionId` = `1790935140000`. seq 1 의 `reportedUserId` = `999` |
| O-D-8 | manual | 묶어 보낸 레코드가 줄 단위로 끊김 (NDJSON) | `aws s3 cp s3://$BUCKET/$KEY - \| gunzip \| grep -c '}{'` = 0 |
| O-D-9 | manual | O-D-6 이후 전송 실패 로그 없음 | 노드에서 `kubectl -n staging logs deploy/yapp-app-staging --since=15m \| grep -c "Firehose putRecordBatch"` = 0 (O-D-6 직후 실행) |
| O-D-10 | manual | 로그 2건이 Firehose 레코드 1건으로 묶여 들어감 (레코드당 5KB 과금 대응) | O-D-6 외 다른 전송이 없던 창에서 `aws cloudwatch get-metric-statistics --namespace AWS/Firehose --metric-name IncomingRecords --dimensions Name=DeliveryStreamName,Value=team-neki-log-raw-staging-client-log --start-time $(date -u -v-15M +%Y-%m-%dT%H:%M:%SZ) --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 900 --statistics Sum --query 'Datapoints[].Sum'` = `[1.0]` |
| O-D-11 | manual | O-D-10 후 토큰 없는 이벤트를 보내면 200 D-0, S3 에서 서버 `userId` 는 null | `http/client-log.http` 의 O-D-11 통과 후 버퍼 시간 경과. `KEY=$(aws s3 ls s3://$BUCKET/$PREFIX --recursive \| sort \| tail -1 \| awk '{print $4}')` 를 다시 실행하고 `aws s3 cp s3://$BUCKET/$KEY - \| gunzip \| grep '"eventType":"oracle_anonymous"' \| jq -c '{userId, reportedUserId: .log.userIdAtOccurrence}'` 에서 두 값 모두 null |

Platform ADR-0004 에 따라 GZIP 으로 적재되므로 O-D-7, O-D-8 은 `gunzip` 을 거칩니다. Loki 는 현재 `prod` 네임스페이스만 수집하므로 staging 로그는 `kubectl` 로 확인합니다.

## O-R. 릴리스

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | `gh pr view feat/BACKEND-166 --json state,baseRefName --jq '.state + " " + .baseRefName'` = `OPEN main` (머지 전) |
| O-R-2 | auto | CI 통과 : `gh pr checks feat/BACKEND-166 --json name,state --jq '.[] \| select(.name=="Test") \| .state'` = `SUCCESS` |
| O-R-3 | manual | O-D 전부 통과 (O-D-5 는 해당 없음 허용) |
| O-R-4 | manual | main 머지 전 prod 스트림 확인. main push 는 `deploy-api-prod.yml` 로 prod 에 바로 배포되므로, `aws firehose describe-delivery-stream --delivery-stream-name team-neki-log-raw-production-client-log` 이 `ACTIVE` 가 아니면 (Platform#23 apply 전) prod 의 `/api/logs` 는 D-15 를 반환함. 클라이언트 연동 전이라 허용한다면 PR 본문에 기록 |
| O-R-5 | manual | 티켓 BACKEND-166 DONE (O-R-3 통과 뒤) |

## 검증 기록

### 2026-10-04 (코드 기준 커밋 `48dd836c`)

Platform#23 apply 직후 기록입니다. Server 는 아직 staging 에 배포하지 않았습니다 (staging 이미지 `1.0.0-88c3b93`, OpenAPI 에 `/api/logs` 없음).

| id | 결과 | 근거 |
|---|---|---|
| O-0-1 | 통과 | `spotlessCheck` 종료코드 0 |
| O-0-2 | 통과 | tests/failures : domain 32/0, apps/batch 18/0, apps/api 585/0 |
| O-0-3 | 통과 | `:apps:api:bootJar` 종료코드 0 |
| O-0-4 | 통과 | 0 |
| O-0-5 | 통과 | 추적 0, ignore 대상 |
| O-A-1 | 통과 | 종료코드 0 |
| O-A-2 | 통과 | 2 |
| O-A-3 | 통과 | 2 |
| O-A-4 | 통과 | 1 |
| O-A-5 | 당시 D-15 계약 통과 | 1. D-16 은 후속 변경으로 새로 검증 |
| O-A-6 | 당시 JWT 필수 계약 통과 | 0. 익명 수집 계약은 후속 변경으로 새로 검증 |
| O-A-7 | 통과 | 종료코드 0 |
| O-D-0 ~ O-D-4 | 미검증 | staging 배포 전 |
| O-D-5 | 해당 없음 | Server 배포 전에 스트림이 생성됨 (Platform#23 apply) |
| O-D-S | 통과 | `ACTIVE`, `arn:aws:s3:::team-neki-log-staging`, `raw/client-log/year=...`, `GZIP`, 300초 |
| O-D-6 ~ O-D-11 | 미검증 | staging 배포 후 |
| O-R-1 | 통과 | `OPEN main` |
| O-R-2 | 통과 | `Test` = `SUCCESS` (Spotless, CodeRabbit 도 통과) |
| O-R-3 | 미검증 | O-D 미완 |
| O-R-4 | 통과 | prod 스트림 `ACTIVE`, 목적지 `team-neki-log-production`. main 머지 후에도 D-15 를 반환하지 않음 |
| O-R-5 | 미검증 | |

**참고 : Platform 측 스모크 테스트.** staging 스트림에 레코드 1건을 직접 `put-record` 했더니 약 320초 뒤 `s3://team-neki-log-staging/raw/client-log/year=2026/month=10/day=03/...gz` 에 쌓였고, 압축을 푼 내용이 보낸 것과 같았습니다. `raw/client-log-errors/` 는 0건입니다. Server 를 거치지 않은 검증이라 O-D-7 을 대신하지는 않지만, 스트림에서 S3 까지의 구간(전송 역할 권한, prefix, GZIP)은 동작함을 보여 줍니다. 날짜 폴더는 UTC 도착 시각 기준이라 KST 10/4 05:20 전송분이 `day=03` 에 들어갔습니다. 이 레코드에는 `"oracle":"BACKEND-166"` 표시가 없어 O-D-7 의 grep 에는 잡히지 않습니다.

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "dev 배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
- auto 전부 통과 + PR 생성 + CI 통과 = 에이전트 측 완료. O-D 와 O-R-3 통과 = 기능 완료
