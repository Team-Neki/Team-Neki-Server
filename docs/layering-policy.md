# 계층 및 타입 소유권 정책

이 문서는 각 계층이 무엇을 책임지고 어떤 타입을 소유하는지에 대해 다룹니다.

## 모듈 구조

```text
core/                    공유 커널. 모든 모듈이 의존할 수 있음
domain/                  도메인 모델·인터페이스와 자기 도메인 기술 구현 어댑터. core + modules(자기 도메인이 쓰는 것만) 의존
apps/api/                api + application + 교차 도메인 호출 어댑터. 실행 모듈
modules/                 외부 의존성 연결 설정 전용
```

도메인별 구조는 다음과 같습니다.

```text
domain/src/main/kotlin/com/neki/domain/<domain>/
├── repository/                영속성 인터페이스
├── client/                    다른 도메인 호출 인터페이스
├── external/                  외부 시스템 인터페이스
├── dto/                       Command, Query
├── models/                    Entity, VO, enum, 인터페이스 입출력 객체
├── service/                   도메인 서비스
└── infra/
    └── persist|cache|storage|security/ 자기 도메인 인터페이스를 구현하는 기술 어댑터

apps/api/src/main/kotlin/com/neki/api/<domain>/
├── api/
│   ├── controller/            REST controller
│   └── dto/                   Request, Response, Converter
├── application/
│   ├── *UseCase.kt            유스케이스
│   └── dto/                   Result, Assembler
└── infra/
    └── client/                다른 도메인 UseCase 를 호출하는 어댑터
```

`infra/client`(교차 도메인 호출)만 apps/api 에 남습니다. domain 모듈이 apps/api 를 의존하면 순환 의존이 생기기 때문입니다. 자기 도메인 인터페이스(repository/external)만 구현하는 기술 어댑터(persist/cache/storage/security)는 인터페이스 옆에 두는 것이 자연스러워 domain 모듈로 옮겼습니다. 그 대가로 domain 모듈이 `modules:*`(postgres, redis, aws, discord, firebase)와 spring-security/oauth2-client 같은 기술 의존성을 갖게 되었습니다 — 도메인이 자기 애그리거트의 영속성·캐시·외부 저장소를 구현하는 데 필요한 만큼만 추가합니다.

## 타입 소유권

| 타입 | 소유 위치 | 역할 |
| --- | --- | --- |
| `Request`, `Response` | `api/dto` | HTTP 표현 |
| `Converter` | `api/dto` | Request→Command/Query, Result→Response |
| `Command`, `Query` | `domain/<domain>/dto` | 유스케이스 입력 |
| `Result` | `application/dto` | 유스케이스 출력 |
| `Assembler` | `application/dto` | 도메인 객체→Result 조립 |
| Entity, VO, enum | `domain/<domain>/models` | 도메인 상태와 규칙 |
| 인터페이스 입출력 객체 | `domain/<domain>/models` | 도메인 인터페이스가 주고받는 값 |

`Result`와 `Response`는 둘 다 출력 DTO이지만 경계가 다릅니다. `Result`는 유스케이스의 반환값이고 `Response`는 HTTP 표현입니다.

## 도메인 서비스와 유스케이스의 책임 경계

가장 자주 어긋나는 지점입니다. 둘 다 로직을 담기 때문에 경계가 흐려지기 쉽습니다.

- 도메인 서비스 : 자기 애그리거트의 불변식과 엔티티 상태 전이
- 유스케이스 : 요청 해석, 트랜잭션 경계, 애그리거트 간 호출 순서, 외부 연동과 보상

이 기준으로 갈리는 예시는 다음과 같습니다.

| 로직 | 소유 | 이유 |
| --- | --- | --- |
| 폴더 이름 중복 검증 | `FolderService` | 폴더 애그리거트의 불변식 |
| 삭제 건수와 요청 건수 일치 검증 | `FolderService` | 폴더 애그리거트의 불변식 |
| 즐겨찾기→사진 삭제 순서 | 유스케이스 | 애그리거트 간 순서 |
| media 가용성 확인 후 보상 롤백 | 유스케이스 | 외부 연동과 보상 |
| `command.deletePhotos` 분기 | 유스케이스 | 요청 해석 |

### 도메인 서비스 규칙

- 자기 도메인의 인터페이스만 의존함. repository 외에 cache·storage·generator 등 자기 도메인 인터페이스는 의존해도 됨. e.g. `PoseService`가 `PoseViewCache`, `RandomGenerator`를 의존
- 다른 도메인 호출 클라이언트(`*Client`)와 다른 도메인 서비스는 의존하지 않음. 교차 도메인 호출은 유스케이스가 함
- 파라미터로 `Command`/`Query`를 그대로 받음. 스칼라로 분해해서 넘기지 않음
- 여러 command가 같은 모양을 공유하면 인터페이스로 묶음. e.g. `FolderCommand.PhotosToTargetFolders`
- 소유자만 필요한 경우 `UserScoped` 계약으로 받음

오케스트레이션 중에 정해지는 값(조회로 얻은 id 목록 등)은 command에 없으므로 `(command, photoIds)` 형태로 함께 받습니다. 이 경우 command에 담기지 않는 값임을 주석으로 밝힙니다.

### 유스케이스 규칙

- 트랜잭션 경계는 유스케이스가 잡음
- 외부 정리(media 삭제 등)는 트랜잭션 커밋 이후에 호출
- `Result` 조립은 `Assembler`에 위임하고 유스케이스에 인라인으로 두지 않음

## 인터페이스 규칙

- 도메인 인터페이스에 `port` 패키지와 `Port` 접미사를 쓰지 않음. 역할별 패키지로 나눔
- 인터페이스의 매개변수와 반환값은 같은 도메인의 `models` 타입만 사용
- Spring Data JPA 인터페이스는 `Jpa*Repository`, QueryDSL은 `*QueryRepository`

역할별 패키지는 세 갈래입니다.

| 패키지 | 담는 것 | 예시 |
| --- | --- | --- |
| `repository/` | 자기 도메인 영속성 | `MediaRepository`, `FolderRepository` |
| `client/` | 다른 도메인 호출 | `photo/client/MediaClient`, `user/client/TermClient` |
| `external/` | 외부 시스템 연동 | `MediaStorage`, `DistributedLock`, `MapApiClient` |

`client/`는 도메인 간 호출 전용입니다. `MapApiClient`, `MapSearch`는 `Client` 접미사가 붙어 있지만 Kakao 외부 API이므로 `external/`에 둡니다. 패키지만 보고 도메인 경계를 넘는 호출인지 판별할 수 있게 하는 것이 목적입니다.

인터페이스가 아닌 순수 도메인 규칙(`BrandOrderPolicy` 같은 object)은 도메인 루트에 둡니다.

구현체 이름은 두 갈래입니다.

- 기술 구현 어댑터 : `*Adapter`. e.g. `RedisDistributedLockAdapter`, `MediaRepositoryAdapter`
- 다른 도메인 호출 어댑터 : `<소비도메인><Provider>Client`. e.g. `PhotoMediaClient`, `MapMediaClient`

인터페이스와 구현체 이름이 같아지지 않게 하는 것이 목적입니다. `Port` 접미사를 뗀 뒤 `AuthTokenProvider`(인터페이스)와 구현체 이름이 충돌해 `AuthTokenProviderAdapter`로 바꾼 사례가 있습니다.

infra 내부에서만 쓰이는 인터페이스는 이 규칙 밖입니다. e.g. `user/infra/cache/AuthCachePort` 도메인 경계를 넘지 않으므로 그대로 두었습니다.

## Contract 를 두지 않습니다

기존에 도메인 간 호출과 조회 프로젝션에 두던 `*Contract` 타입은 전량 제거했습니다.

`Contract`는 네 가지 서로 다른 것을 한 이름에 담고 있었습니다.

- 외부 클라이언트 응답 형식
- 리포지터리 조회 프로젝션
- 도메인이 계산한 판정 결과
- 페이지·인덱스 같은 구조적 개념

뒤의 두 가지는 애초에 외부와의 계약이 아니었습니다. 넷 다 도메인 값 객체(`models`)로 옮기고 `Contract` 이름을 없앴습니다.

### 도메인 간 호출 흐름

```text
Provider Application Result -> Consumer Infra Adapter -> Consumer Domain Model
```

변환은 소비 도메인의 infra adapter가 담당합니다. 중간 전송 타입을 따로 두지 않고 provider의 `Result`에서 소비 도메인 모델로 바로 변환합니다.

모놀리식 구조에서는 provider의 use case를 직접 호출하므로 중간 타입이 순수한 우회가 됩니다. 서비스가 분리되면 그 시점에 HTTP·메시지 DTO가 adapter 안에 생기고, 도메인 경계는 지금과 동일하게 유지됩니다.

- 호출 인터페이스는 소비 도메인이 소유함. e.g. `photo.MediaClient`
- 호출 인터페이스는 소비 도메인의 모델을 반환함. e.g. `photo.models.MediaMetadata`
- provider의 Entity, `Result`, `Response`를 소비 도메인이 직접 참조하지 않음

같은 모양의 모델이 도메인마다 중복되는 트레이드 오프가 있습니다. e.g. `photo.models.MediaMetadata`와 `pose.models.MediaMetadata` 도메인 격리를 위해 감수하는 중복입니다.

## 엔티티 상태 변경

엔티티 필드를 외부에서 직접 대입하지 않고 의도가 드러나는 메서드로 변경합니다.

```kotlin
// 지양
folder.name = command.newName
appVersion.minVersion = command.minVersion

// 지향
folder.rename(command.newName)
appVersion.updateVersions(command.minVersion, command.currentVersion)
```

Kotlin은 주 생성자 프로퍼티에 `private set`을 붙일 수 없어 `var`는 public으로 남습니다. 따라서 이 규칙은 컴파일러가 강제하지 못하고 관례로만 유지됩니다.

이력처럼 종류가 갈리는 엔티티는 companion factory로 의도를 드러냅니다.

```kotlin
UserTermAgreementHist.agreed(userId, termId)
UserTermAgreementHist.withdrawn(userId, termId)
```

## 일급 컬렉션 도입 기준

컬렉션을 감싸는 것 자체가 목적이 아닙니다. **그 컬렉션에 대한 질의나 판단이 반복될 때만 도입합니다.**

- 도입함 : `ActiveTerms` 필수 여부, 마케팅 약관 조회, 활성 여부 검증이 여러 번 반복됨
- 도입함 : `MediaMetadatas` mediaId로 찾는 인덱싱이 여러 조립기에서 반복됨
- 도입함 : `MediaAvailabilities` 가용 여부 판정과 롤백 대상 추출이 함께 다뤄짐
- 도입 안 함 : `List<NotificationHist>` 한 번 매핑하고 끝. 감싸면 껍데기만 늘어남
- 도입 안 함 : `savePhotos(photos)` 리스트가 그냥 통과함

유스케이스의 리스트 매핑이 거슬린다면 그것은 일급 컬렉션이 아니라 `Assembler`로 해결할 문제입니다.

## Assembler 와 Converter

계층별로 변환자 이름이 갈립니다.

- `api/dto/*Converter` : Request→Command/Query, Result→Response
- `application/dto/*Assembler` : 도메인 객체→Result

`Assembler`는 역할상 mapper이지만 두 소스를 결합하는 경우가 있어 이 이름을 씁니다. e.g. 사진과 media 메타데이터를 결합하고 짝이 없는 항목은 제외

포트 호출은 유스케이스가 하고 `Assembler`는 순수 변환만 합니다. 조립에 외부 조회가 필요하면 유스케이스가 먼저 조회해서 넘깁니다.

## 공유 커널

`core`의 `com.neki.common.domain.vo`에 도메인 무관한 값 객체를 둡니다.

- `SortOrder` : 정렬 순서
- `Pagination` : 페이지 조회 조건. `offset`, `limit`, `slice()` 를 소유
- `Page<T>` : 조회 결과와 다음 페이지 존재 여부

`Pagination`이 `limit = size + 1`과 슬라이싱을 함께 소유하므로 hasNext 판단 전략이 한 곳에만 서술됩니다.

도메인 타입을 참조하지 않는 순수 구조적 개념만 여기에 둡니다. 도메인 의미가 붙는 순간 각 도메인의 `models`로 가야 합니다.

## ArchUnit 규칙

`apps/api/src/test/kotlin/com/neki/rule/ArchitectureRulesTest.kt`가 이 정책을 검증합니다.

- 도메인 격리 : api·application·엔티티·domain infra 계층이 다른 도메인을 의존하지 않음
- 계층 의존 : application이 api·infra를 의존하지 않음, domain(models/dto/service/repository/client/external)이 자기 도메인 infra를 의존하지 않음
- DTO 배치 : Request·Response는 api, Command·Query는 domain dto, Result는 application
- 어노테이션 배치 : `@UseCase`는 application, `@RestController`는 api.controller, `@Repository`는 infra

DTO 배치 규칙은 이름 접미사만 보고 판단하므로 접미사가 우연히 겹치는 공유 커널 값 객체까지 걸립니다. 따라서 `com.neki.common.domain..`은 검사 대상에서 제외합니다.

## 금지 사항

- application이 domain 모델을 직접 반환하는 것
- api가 domain 모델을 직접 응답으로 노출하는 것
- 도메인 서비스가 다른 도메인 서비스나 외부 클라이언트를 의존하는 것
- 도메인 인터페이스가 `Result`나 다른 도메인 타입을 주고받는 것
- domain에서 `infra` 타입을 import하는 것
- 유스케이스가 command를 스칼라로 분해해 도메인 서비스에 넘기는 것

## 미결 사항

- `PhotoBoothLocation.location`이 JTS `Point`를 그대로 노출합니다. `Point`는 가변 객체이므로 방어적 복사나 값 객체 래핑이 정석이지만, 좌표 연산 코드가 `Point`를 전제로 작성되어 있습니다.
- 엔티티 `var` 프로퍼티를 실제로 막으려면 모든 엔티티의 생성자 구조를 바꿔야 합니다.
