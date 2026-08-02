---
name: architecture
description: Load when designing new domains or modules, refactoring architecture, working with Clean Architecture layers, ports/adapters pattern, or cross-domain communication.
---

# Architecture & Design Patterns

Load this context when designing features, creating new domains, or refactoring.

---

## Clean Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  API Layer (Controllers, DTOs, Converters)                  │
│  - Handles HTTP requests/responses                          │
│  - Input validation                                         │
│  - DTO transformation                                       │
├─────────────────────────────────────────────────────────────┤
│  Application Layer (UseCases, Commands, Results, Ports)     │
│  - Business logic orchestration                             │
│  - Transaction management                                   │
│  - Defines ports (interfaces) for infrastructure            │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer (Entities, Value Objects, Enums)              │
│  - Core business rules                                      │
│  - Domain models                                            │
│  - No external dependencies                                 │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Layer (Adapters, Repositories, Configs)     │
│  - Implements ports defined in application layer            │
│  - External service integration                             │
│  - Database access                                          │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Rules

| Layer          | Can Depend On       | Cannot Depend On          |
|----------------|---------------------|---------------------------|
| API            | Application, Domain | Infrastructure (directly) |
| Application    | Domain              | API, Infrastructure       |
| Domain         | Nothing             | Any other layer           |
| Infrastructure | Application, Domain | API                       |

**Critical**: Inner layers MUST NOT depend on outer layers.

- ✅ `application` → `domain`
- ❌ `application` → `api` or `infra`

---

## Domain Module Structure

```
core/                     # 공유 커널 (annotation, code, domain, exception, transaction)
domain/                   # JPA 엔티티. com.neki.<context>.entity.*
apps/api/              # api + application + infra 어댑터. 실행 모듈(bootJar)
└── src/main/kotlin/com/neki/
    ├── user/                  # 회원, 인증(OAuth/JWT)
    │   ├── api/              # Controllers, DTOs, Converters
    │   ├── application/      # UseCases, dto, Ports
    │   └── infra/            # Adapters, Security configs
    ├── photo/                 # 사진 아카이빙
    ├── pose/                  # 포즈 추천
    ├── map/                   # 부스 위치 검색
    ├── media/                 # S3 미디어
    ├── support/               # 약관, 앱 버전
    ├── notification/          # 푸시, Discord
    └── common/                # 필터, 프로퍼티, 예외 핸들러 등 실행 모듈 공통
modules/                       # 외부 의존성 연결 설정 전용
├── postgres/  redis/  aws/  kakao/  apple/  discord/  jasypt/  firebase/
```

도메인 엔티티는 `:domain` 에 있으므로 도메인별 `domain/` 하위 패키지는 존재하지 않는다.
`com.neki.<context>.entity.*` 로 바로 접근한다.

### Per-Domain Structure

```
[domain]/
├── api/
│   ├── controller/     # REST controllers
│   ├── converter/      # Request→Command, Result→Response
│   └── dto/           # Request/Response DTOs
├── application/
│   ├── command/       # Input to use cases
│   ├── result/        # Output from use cases
│   ├── port/          # Interfaces for infrastructure
│   └── usecase/       # Business logic
├── domain/
│   ├── entity/        # JPA entities
│   └── enums/         # Domain enums
└── infra/
    └── persist/       # Repository adapters
        └── jpa/       # JPA repositories
```

---

## Domain Isolation Rule

**Domains MUST NOT import from other domains directly.**

❌ **Wrong**:

```kotlin
// In photo domain
import com.neki.user.entity.User  // Direct import!
```

✅ **Correct** - Use ports for cross-domain communication:

```kotlin
// In photo domain - define a port
interface UserInfoPort {
    fun getUserName(userId: Long): String
}

// In user domain - implement the port
@Component
class UserInfoAdapter(
    private val userRepository: UserRepository
) : UserInfoPort {
    override fun getUserName(userId: Long): String {
        return userRepository.findById(userId).name
    }
}
```

---

## UseCase Pattern

Services are annotated with `@UseCase`:

```kotlin
@UseCase
class CreateFolderUseCase(
    private val folderRepository: FolderRepositoryPort  // Inject port, not adapter
) {
    @Transactional
    fun execute(command: CreateFolderCommand): CreateFolderResult {
        // 1. Validate business rules
        if (folderRepository.existsOwnedFolderName(command.userId, command.name)) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        // 2. Create domain entity
        val folder = Folder(
            userId = command.userId,
            name = command.name,
        )

        // 3. Persist
        val saved = folderRepository.save(folder)

        // 4. Return result
        return CreateFolderResult(saved.id!!)
    }
}
```

Reference: `core/src/main/kotlin/com/neki/common/annotation/UseCase.kt`

---

## Port/Adapter Pattern

### Port (Interface in Application Layer)

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/application/port/FolderRepositoryPort.kt
interface FolderRepositoryPort {
    fun save(folder: Folder): Folder
    fun findById(id: Long): Folder?
    fun findAllByUserId(userId: Long): List<Folder>
    fun existsOwnedFolderName(userId: Long, name: String): Boolean
    fun deleteById(id: Long)
}
```

### Adapter (Implementation in Infrastructure Layer)

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/infra/persist/FolderRepositoryAdapter.kt
@Repository
class FolderRepositoryAdapter(
    private val jpaRepository: JpaFolderRepository
) : FolderRepositoryPort {

    override fun save(folder: Folder): Folder {
        return jpaRepository.save(folder)
    }

    override fun findById(id: Long): Folder? {
        return jpaRepository.findByIdOrNull(id)
    }

    // ... other implementations
}
```

### JPA Repository

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/infra/persist/jpa/JpaFolderRepository.kt
interface JpaFolderRepository : JpaRepository<Folder, Long> {
    fun findAllByUserId(userId: Long): List<Folder>
    fun existsByUserIdAndName(userId: Long, name: String): Boolean
}
```

### Port Method Naming Conventions

Use consistent verb names across all ports:

| Operation | Method Name               | Example                              |
|-----------|---------------------------|--------------------------------------|
| Create    | `add`, `save`, `create`   | `add(userId, photoId)`               |
| Read      | `find*`, `get*`, `exists` | `findById(id)`, `existsByName(name)` |
| Update    | `update`, `modify`        | `update(entity)`                     |
| Delete    | `delete`, `remove`        | `delete(userId, photoId)`            |
| Count     | `count*`                  | `countByUserId(userId)`              |

**Prefer `delete` over `remove` for consistency with SQL terminology.**

---

## Command/Query/Result Pattern

application DTO는 모두 `application/dto/` 에 두고, 도메인 그룹별 `object` 하위 중첩 클래스로 묶는다.
쓰기 입력은 `XxxCommand`, 조회 입력은 `XxxQuery`, 출력은 `XxxResult`.

### Command (쓰기 입력)

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/application/dto/FolderCommand.kt
object FolderCommand {
    data class CreateFolder(
        val userId: Long,
        val name: String,
    )

    data class DeleteFolders(
        val userId: Long,
        val folderIds: List<Long>,
    )
}
```

### Query (조회 입력)

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/application/dto/FolderQuery.kt
object FolderQuery {
    data class GetFolders(
        val userId: Long,
        val limit: Int?,
    )
}
```

### Result (출력)

```kotlin
// apps/api/src/main/kotlin/com/neki/photo/application/dto/FolderResult.kt
object FolderResult {
    data class CreateFolder(
        val folderId: Long,
    )

    data class GetFolders(
        val items: List<FolderInfo>,
    ) {
        data class FolderInfo(val folderId: Long, val name: String, val storageKey: String?, val count: Long)
    }
}
```

---

## QueryDSL for Batch Operations

When you need batch operations (delete/update multiple records), use QueryDSL instead of Spring Data
JPA for better performance.

### Naming Convention

- **Spring Data JPA**: `Jpa*Repository` (e.g., `JpaFolderRepository`)
- **QueryDSL**: `*QueryRepository` (e.g., `FolderQueryRepository`)

### Example: Batch Delete

```kotlin
// QueryDSL repository
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

// Adapter using both JPA and QueryDSL
@Repository
class FavoriteImageRepositoryAdapter(
    private val jpaRepository: JpaFavoriteImageRepository,
    private val queryRepository: FavoritePhotoQueryRepository,
) : FavoriteImageRepositoryPort {

    override fun delete(favoritePhoto: FavoritePhoto) =
        jpaRepository.deleteById(favoritePhoto.id)

    override fun deleteAll(userId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        queryRepository.deleteAllByUserIdAndPhotoIds(userId, photoIds)
    }
}
```

**Performance**: Single DELETE query vs N individual deletes

---

## Entity Deletion Patterns

### Cascade Deletion Order

When deleting entities with relationships, delete dependent entities FIRST to prevent orphan
records.

**Example: Photo with Favorites**

```kotlin
@UseCase
class DeletePhotoUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {
    fun execute(command: DeletePhotoCommand) {
        val photo = transactionRunner.run {
            // 1. Delete favorites FIRST (dependent)
            favoriteImageRepository.delete(FavoritePhoto(command.userId, command.photoId))

            // 2. Delete photo SECOND (parent)
            photoImageRepository.deleteOwnedPhoto(command.userId, command.photoId)
        } ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 3. External cleanup LAST (outside transaction)
        mediaClient.deleteMedia(command.userId, photo.mediaId)
    }
}
```

**Key Points:**

- Delete dependent entities before parent entities
- Use transactions to ensure atomicity
- External service calls (S3, etc.) happen AFTER transaction commits
- Prevents orphan records in the database

---

## Checklist for New Domain

- [ ] Create domain directory with api/application/domain/infra structure
- [ ] Define entities in `domain/entity/`
- [ ] Define ports in `application/port/`
- [ ] Implement adapters in `infra/persist/`
- [ ] Create use cases with `@UseCase` annotation
- [ ] Create converters for Request→Command and Result→Response
- [ ] Ensure NO imports from other domains
- [ ] Add E2E tests

---

## File References

| Component          | Location                                                           |
|--------------------|--------------------------------------------------------------------|
| UseCase annotation | `core/src/main/kotlin/com/neki/common/annotation/UseCase.kt`            |
| Base entity        | `core/src/main/kotlin/com/neki/common/domain/BaseTimeEntity.kt`         |
| Transaction runner | `core/src/main/kotlin/com/neki/common/transaction/TransactionRunner.kt` |
