---
name: architecture
description: Load when designing new domains or modules, refactoring architecture, working with Clean Architecture layers, domain services, cross-domain communication, or DTO placement.
---

# Architecture & Design Patterns

Load this context when designing features, creating new domains, or refactoring.

전체 정책과 결정 근거는 `docs/layering-policy.md` 에 있다. 이 문서는 작업 중 자주 쓰는 부분만 추린다.

---

## Module Structure

```text
core/          공유 커널. annotation, code, exception, transaction, domain/vo
domain/        도메인 모델·인터페이스와 자기 도메인 기술 구현 어댑터. core + modules(자기 도메인이 쓰는 것만) 의존
apps/api/      api + application + (교차 도메인 호출 어댑터). 실행 모듈(bootJar)
modules/       외부 의존성 연결 설정 전용 (postgres, redis, aws, kakao, apple, discord, jasypt, firebase)
```

### Per-Domain Structure

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
│   ├── *UseCase.kt            유스케이스 (usecase/ 하위 패키지 없음)
│   └── dto/                   Result, Assembler
└── infra/
    └── client/                다른 도메인 UseCase 를 호출하는 어댑터
```

`infra/client` 는 다른 도메인의 UseCase(apps/api)를 호출하므로 apps/api 에 남는다. domain 모듈이 apps/api 를 의존하면 순환 의존이 생기기 때문이다. 그 외 자기 도메인 인터페이스(repository/external)만 구현하는 기술 어댑터는 domain 모듈로 옮겨져 있다.

---

## Dependency Rules

| Layer                    | Can Depend On                  | Cannot Depend On |
|---------------------------|--------------------------------|-------------------|
| API                       | Application, Domain            | Infrastructure    |
| Application               | Domain                         | API, Infrastructure (client 제외) |
| Domain (models/dto/service/repository/client/external) | Core | 자기 도메인 infra, 다른 도메인, Application, API |
| Domain infra (persist/cache/storage/security) | Core, modules:* | 다른 도메인, Application, API |
| apps/api infra/client      | Application(다른 도메인), Domain | API |

`apps/api/src/test/kotlin/com/neki/api/rule/ArchitectureRulesTest.kt` 가 이를 검증한다.

---

## 도메인 서비스 vs 유스케이스

가장 자주 어긋나는 경계다.

- 도메인 서비스 : 자기 애그리거트의 불변식과 엔티티 상태 전이
- 유스케이스 : 요청 해석, 트랜잭션 경계, 애그리거트 간 호출 순서, 외부 연동과 보상

```kotlin
@UseCase
class DeletePhotosUseCase(
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {
    fun execute(command: PhotoImageCommand.DeletePhotos) {
        val deletedPhotos: List<PhotoImage> = transactionRunner.run {
            // 즐겨찾기를 먼저 지워야 고아 레코드가 남지 않는다 (애그리거트 간 순서 = 유스케이스)
            favoriteService.removeAll(command)

            photoService.deletePhotos(command)
        }

        // 외부 정리는 트랜잭션 커밋 이후
        mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
    }
}
```

### 도메인 서비스 규칙

- 자기 도메인의 인터페이스만 의존한다. repository 외에 cache·storage·generator 도 자기 도메인 것이면 무방하다
- 다른 도메인 호출 클라이언트(`*Client`)와 다른 도메인 서비스는 의존하지 않는다. 교차 도메인 호출은 유스케이스가 한다
- `Command`/`Query` 를 그대로 받는다. 스칼라로 분해해서 넘기지 않는다

```kotlin
// 지양
photoService.validatePhotosOwned(command.userId, command.photoIds)

// 지향
photoService.validatePhotosOwned(command)
```

여러 command 가 같은 모양을 공유하면 인터페이스로 묶는다.

```kotlin
object FolderCommand {
    interface PhotosToTargetFolders : UserScoped {
        val photoIds: List<Long>
        val targetFolderIds: List<Long>
    }
}
```

소유자만 필요하면 `UserScoped` 로 받는다. 오케스트레이션 중 정해지는 값은 `(command, photoIds)` 형태로 함께 받고 주석을 남긴다.

---

## Domain Isolation

도메인은 다른 도메인을 직접 import 하지 않는다. 소비 도메인이 호출 인터페이스를 소유하고, 자기 도메인의 모델을 반환한다.

```kotlin
// domain/photo/client/MediaClient.kt — 소비 도메인이 소유
interface MediaClient {
    fun getMediaMetadata(ownerId: Long, mediaIds: List<Long>): List<MediaMetadata>  // photo.models
}

// apps/api/photo/infra/client/PhotoMediaClient.kt — 어댑터가 변환 담당
@Component
class PhotoMediaClient(private val getMediaMetadataListUseCase: GetMediaMetadataListUseCase) : MediaClient {

    override fun getMediaMetadata(ownerId: Long, mediaIds: List<Long>): List<MediaMetadata> {
        val result: MediaResult.GetMediaMetadataList =
            getMediaMetadataListUseCase.execute(MediaQuery.GetMediaMetadataList(ownerId, mediaIds))

        return result.medias.map { it.toMetadata() }
    }

    // media 도메인의 Result 를 photo 도메인 모델로 변환
    private fun MediaResult.Metadata.toMetadata(): MediaMetadata = MediaMetadata(...)
}
```

`*Contract` 타입은 두지 않는다. provider 의 `Result` 에서 소비 도메인 모델로 adapter 가 바로 변환한다.

같은 모양의 모델이 도메인마다 중복되는 것은 도메인 격리를 위해 감수하는 트레이드 오프다.

---

## Interface Naming

- 도메인 인터페이스에 `port` 패키지와 `Port` 접미사를 쓰지 않는다. 역할별로 `repository/`, `client/`, `external/` 에 나눠 둔다
- `client/` 는 도메인 간 호출 전용이다. `MapApiClient` 처럼 `Client` 접미사가 붙어도 외부 API 면 `external/` 에 둔다
- 기술 구현 어댑터는 `*Adapter`, 다른 도메인 호출 어댑터는 `<소비도메인><Provider>Client` (e.g. `PhotoMediaClient`)
- Spring Data JPA 는 `Jpa*Repository`, QueryDSL 은 `*QueryRepository`
- infra 내부에서만 쓰이는 인터페이스는 이 규칙 밖이다 (e.g. `user/infra/cache/AuthCachePort`)

| Operation | Method Name               | Example                              |
|-----------|---------------------------|--------------------------------------|
| Create    | `add`, `save`, `create`   | `add(userId, photoId)`               |
| Read      | `find*`, `get*`, `exists` | `findById(id)`, `existsByName(name)` |
| Update    | `update`, `modify`        | `update(entity)`                     |
| Delete    | `delete`, `remove`        | `delete(userId, photoId)`            |
| Count     | `count*`                  | `countByUserId(userId)`              |

---

## Command / Query / Result / Assembler

| 타입 | 위치 |
|------|------|
| `Command`, `Query` | `domain/<domain>/dto` |
| `Result`, `Assembler` | `apps/api/<domain>/application/dto` |
| `Request`, `Response`, `Converter` | `apps/api/<domain>/api/dto` |

```kotlin
// domain/photo/dto/FolderCommand.kt
object FolderCommand {
    data class CreateFolder(override val userId: Long, val name: String) : UserScoped
}

// apps/api/photo/application/dto/FolderResult.kt
object FolderResult {
    data class CreateFolder(val folderId: Long)
}
```

### Assembler

`Result` 조립은 유스케이스에 인라인으로 두지 않고 `application/dto/*Assembler` 로 뺀다.

```kotlin
object FolderAssembler {
    fun toItems(foldersWithStats: List<FolderStats>): List<FolderResult.GetFolders.Item> = ...
}
```

포트 호출은 유스케이스가 하고 `Assembler` 는 순수 변환만 한다. 조립에 외부 조회가 필요하면 유스케이스가 먼저 조회해서 넘긴다.

---

## Entity State Change

엔티티 필드를 외부에서 직접 대입하지 않고 의도가 드러나는 메서드로 바꾼다.

```kotlin
// 지양
folder.name = command.newName

// 지향
folder.rename(command.newName)
```

종류가 갈리는 엔티티는 companion factory 로 의도를 드러낸다.

```kotlin
UserTermAgreementHist.agreed(userId, termId)
UserTermAgreementHist.withdrawn(userId, termId)
```

---

## 일급 컬렉션 도입 기준

컬렉션에 대한 질의나 판단이 **반복될 때만** 도입한다. 한 번 매핑하고 끝나는 리스트를 감싸면 껍데기만 늘어난다.

- 도입 : `ActiveTerms`, `MediaMetadatas`, `MediaAvailabilities`
- 미도입 : `List<NotificationHist>` (한 번 매핑하고 끝)

유스케이스의 리스트 매핑이 거슬린다면 일급 컬렉션이 아니라 `Assembler` 로 해결할 문제다.

---

## Shared Kernel

`core` 의 `com.neki.common.domain.vo` 에 도메인 무관한 값 객체를 둔다.

- `SortOrder` : 정렬 순서
- `Pagination` : `offset`, `limit`, `slice()` 를 소유. hasNext 판단 전략이 여기 한 곳에만 있다
- `Page<T>` : 조회 결과와 다음 페이지 존재 여부

도메인 의미가 붙는 순간 각 도메인의 `models` 로 가야 한다.

---

## QueryDSL for Batch Operations

벌크 삭제·수정은 Spring Data JPA 대신 QueryDSL 을 쓴다. 단일 쿼리로 처리되어 N 번의 개별 쿼리를 피할 수 있다.

```kotlin
@Repository
class FavoritePhotoQueryRepository(private val queryFactory: JPAQueryFactory) {
    fun deleteAllByUserIdAndPhotoIds(userId: Long, photoIds: List<Long>): Long =
        queryFactory.delete(favoritePhoto)
            .where(
                favoritePhoto.id.userId.eq(userId),
                favoritePhoto.id.photoId.`in`(photoIds),
            )
            .execute()
}
```

---

## Checklist for New Domain

- [ ] `domain/<domain>/models` 에 엔티티와 값 객체 정의
- [ ] `domain/<domain>/dto` 에 Command, Query 정의
- [ ] 도메인 인터페이스를 `domain/<domain>/{repository,client,external}` 에 정의 (Port 접미사 없이)
- [ ] `domain/<domain>/service` 에 도메인 서비스 정의 (자기 애그리거트 리포지터리만 의존)
- [ ] `domain/<domain>/infra` 에 자기 도메인 인터페이스(repository/external)를 구현하는 `*Adapter` 구현
- [ ] `apps/api/<domain>/application` 에 `@UseCase` 정의 (command/query 를 그대로 도메인 서비스에 전달)
- [ ] `apps/api/<domain>/application/dto` 에 Result 와 Assembler 정의
- [ ] `apps/api/<domain>/infra/client` 에 다른 도메인 UseCase 를 호출하는 어댑터 구현
- [ ] `apps/api/<domain>/api/dto` 에 Request, Response, Converter 정의
- [ ] 다른 도메인 import 가 없는지 확인
- [ ] E2E 테스트 추가

---

## File References

| Component          | Location                                                               |
|--------------------|------------------------------------------------------------------------|
| UseCase annotation | `core/src/main/kotlin/com/neki/common/annotation/UseCase.kt`            |
| Base entity        | `core/src/main/kotlin/com/neki/common/domain/BaseTimeEntity.kt`         |
| Transaction runner | `core/src/main/kotlin/com/neki/common/transaction/TransactionRunner.kt` |
| Paging value objects | `core/src/main/kotlin/com/neki/common/domain/vo/Paging.kt`            |
| Architecture rules | `apps/api/src/test/kotlin/com/neki/rule/ArchitectureRulesTest.kt`       |
| Full policy        | `docs/layering-policy.md`                                              |
