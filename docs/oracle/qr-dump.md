# 오라클 : QR 파싱 실패 HTML 덤프 업로드 ticket (BACKEND-167)

이 문서는 BACKEND-167 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다.

## O-0. 공통

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과 | `./gradlew test -q` 종료코드 0 |
| O-0-3 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |

## O-A. 엔드포인트

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | E2E 통과 (key 형식, contentType, Media 미생성, 비인증 403) : `./gradlew :apps:api:test --tests 'com.neki.api.e2e.media.GenerateQrDumpUploadTicketE2ETest' -q` 종료코드 0 |
| O-A-2 | auto | `MediaType` 에 덤프 타입을 추가하지 않음 (`/api/media/upload` 로 고를 수 없음) : `grep -ci "qr" domain/src/main/kotlin/com/neki/domain/media/models/MediaType.kt` = 0 |
| O-A-3 | auto | 스키마 변경 없음 : `git diff --name-only origin/main...HEAD -- modules/postgres \| wc -l` = 0 |
| O-A-4 | manual | 로컬 localstack 왕복 : `docker compose up -d`, `./gradlew :apps:api:bootRun` 뒤 ticket 발급 -> `curl -X PUT -H "Content-Type: text/html; charset=utf-8" --data-binary @dump.html "$URL"` 이 200 -> `aws --endpoint-url http://localhost:4566 s3 ls s3://yapp-local/qr-dumps/ --recursive` 에 객체 존재. localstack 은 presigned 서명을 검증하지 않으므로 Content-Type 불일치 403 은 여기서 확인할 수 없음 |

## O-R. 릴리스

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | CI 통과 : `gh pr checks feat/BACKEND-167` 전부 pass |
| O-R-2 | manual | staging 배포 후 ticket 발급 -> 응답 `contentType` 그대로 PUT 하면 200 이고 `qr-dumps/{오늘}/{userId}/` 아래 객체 생성. 같은 URL 에 `Content-Type: text/html` 로 PUT 하면 403 (`SignatureDoesNotMatch`) |
| O-R-3 | manual | 티켓 BACKEND-167 DONE (배포 뒤) |

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
