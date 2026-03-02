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
src/main/kotlin/com/neki/
├── auth/              # Authentication domain
│   ├── api/          # Controllers, DTOs
│   ├── application/  # UseCases, Commands, Ports
│   ├── domain/       # Entities, Enums
│   └── infra/        # Adapters, Security configs
├── user/              # User domain
├── photo/             # Photo archiving domain
├── media/             # Media storage domain
└── common/            # Shared utilities
```

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
import com.neki.user.domain.entity.User  // Direct import!
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

Reference: `src/main/kotlin/com/neki/common/annotation/UseCase.kt`

---

## Port/Adapter Pattern

### Port (Interface in Application Layer)

```kotlin
// src/main/kotlin/com/neki/photo/application/port/FolderRepositoryPort.kt
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
// src/main/kotlin/com/neki/photo/infra/persist/FolderRepositoryAdapter.kt
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
// src/main/kotlin/com/neki/photo/infra/persist/jpa/JpaFolderRepository.kt
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

## Command/Result Pattern

### Command (Input)

```kotlin
// src/main/kotlin/com/neki/photo/application/command/FolderCommand.kt
data class CreateFolderCommand(
    val userId: Long,
    val name: String,
)

data class DeleteFolderCommand(
    val userId: Long,
    val folderId: Long,
)
```

### Result (Output)

```kotlin
// src/main/kotlin/com/neki/photo/application/result/FolderResult.kt
data class CreateFolderResult(
    val folderId: Long,
)

data class GetFoldersResult(
    val folders: List<FolderInfo>,
)
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

    override fun delete(userId: Long, photoId: Long) =
        jpaRepository.deleteById(FavoritePhotoId(userId, photoId))

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
            favoriteImageRepository.delete(command.userId, command.photoId)

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
| UseCase annotation | `src/main/kotlin/com/neki/common/annotation/UseCase.kt`            |
| Base entity        | `src/main/kotlin/com/neki/common/domain/BaseTimeEntity.kt`         |
| Transaction runner | `src/main/kotlin/com/neki/common/transaction/TransactionRunner.kt` |
