# 행동 이벤트 수집 계약

`POST /api/logs` 는 앱에서 발생한 행동 이벤트를 배치로 받아 Firehose 에 접수하고 S3 `raw/client-log/` 에 NDJSON 으로 저장한다. S3 에서 Amplitude 로 가져가는 단계는 이 작업에 포함되지 않는다.

```json
{
  "platform": "IOS",
  "logs": [{
    "eventId": "2f8b6c2e-3d4a-4f5b-9c1d-2e7a8b9c0d1e",
    "eventType": "photo_upload",
    "eventProperties": {"method": "qr", "count": 1},
    "occurredAt": "2026-10-02T10:00:00Z",
    "sessionId": 1790935140000,
    "sequenceNumber": 1,
    "deviceId": "stable-device-id",
    "userIdAtOccurrence": null,
    "appVersion": "1.4.0",
    "context": {"appBuild": "140", "osName": "iOS", "osVersion": "18.0", "deviceModel": "iPhone17,1"}
  }]
}
```

한 요청은 1~500개 이벤트를 담는다. `eventId` 는 이벤트 생성 시 UUID 로 만들고 재전송해도 유지한다. `eventType` 은 기존 Amplitude 커스텀 이벤트 이름을 사용한다. `eventProperties` 와 `context` 는 JSON 타입을 유지하며, 서버는 알지 못하는 키도 보존한다. `occurredAt`, `appVersion`, `context` 는 **발생 당시** 값을 이벤트와 함께 저장한다. `receivedAt` 은 서버가 별도로 붙이는 접수 시각이다.

`deviceId` 는 설치에서 안정적으로 유지하는 식별자다. `sessionId` 는 분석 세션 시작 시각의 epoch milliseconds 이며, 같은 세션의 이벤트에 같은 값을 넣는다. `sequenceNumber` 는 같은 기기·세션에서 이벤트 생성 시 1부터 증가시킨다. 세 값은 전송할 때 다시 계산하지 않는다. 앱의 포그라운드·백그라운드와 오프라인 큐는 서버가 알 수 없으므로 세션은 앱에서 관리한다. 기존 Amplitude 세션 기준을 유지하려면 두 앱의 현재 SDK 설정과 세션 종료 조건을 각각 확인한다. [Amplitude 세션 규칙](https://amplitude.com/docs/data/sources/instrument-track-sessions)

Authorization 은 선택이다. 헤더를 보냈다면 유효한 Bearer 토큰이어야 한다. S3 줄의 바깥 `userId` 는 **전송 시점** JWT 사용자 ID이며 익명 전송이면 null 이다. 안쪽 `log.userIdAtOccurrence` 는 앱이 이벤트 발생 당시 기록한 값으로, 서버가 인증한 ID로 취급하지 않는다. 계정 전환 뒤 전송하는 큐의 이벤트를 현재 JWT 사용자로 덮어쓰지 않는다.

앱은 이벤트를 영속 큐에 먼저 기록하고 접수 성공(`200 D-0`) 후 제거한다. `503 D-15` 또는 응답을 못 받은 경우 같은 `eventId` 로 전체 배치를 다시 보낸다. `413 D-16` 이면 배치를 나눠 보낸다. 단일 이벤트가 한도를 초과하면 무한 재시도하지 말고 앱 진단에 남긴다. `200` 은 Firehose 접수이며 S3 파일 생성 완료는 아니다. 재전송으로 생긴 중복은 원본을 보존하고 분석 단계에서 `(deviceId, eventId)` 로 제거한다. [Firehose 부분 실패·중복](https://docs.aws.amazon.com/firehose/latest/APIReference/API_PutRecordBatch.html)

현재 iOS 는 Amplitude `.appLifecycles`, Android 는 `SESSIONS` 와 `APP_LIFECYCLES` 자동 수집을 켠다. 앱의 `track` 호출에서 보내는 커스텀 이벤트만 새 경로로 옮기면 SDK 자동 이벤트와 기기 정보는 빠진다. SDK 직접 전송을 끄기 전에 두 앱의 이벤트 이름·속성, 설치·업데이트·실행·백그라운드·세션 이벤트, 필요한 기기·OS 속성을 대조하고 유지할 항목을 `eventType` 과 `context` 로 명시적으로 기록한다. [iOS 설정](https://github.com/Team-Neki/Neki-iOS/blob/develop/Neki-iOS/Core/Sources/Analytics/Sources/Data/Sources/Implementations/AmplitudeAnalyticsRepository.swift), [Android 설정](https://github.com/Team-Neki/Team-Neki-Android/blob/develop/core/analytics/src/main/kotlin/com/neki/android/core/analytics/logger/AnalyticsModule.kt)
