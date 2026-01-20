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

| Layer | Can Depend On | Cannot Depend On |
|-------|---------------|------------------|
| API | Application, Domain | Infrastructure (directly) |
| Application | Domain | API, Infrastructure |
| Domain | Nothing | Any other layer |
| Infrastructure | Application, Domain | API |

**Critical**: Inner layers MUST NOT depend on outer layers.
- ✅ `application` → `domain`
- ❌ `application` → `api` or `infra`

---

## Domain Module Structure

```
src/main/kotlin/com/yapp2app/
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
import com.yapp2app.user.domain.entity.User  // Direct import!
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

Reference: `src/main/kotlin/com/yapp2app/common/annotation/UseCase.kt`

---

## Port/Adapter Pattern

### Port (Interface in Application Layer)

```kotlin
// src/main/kotlin/com/yapp2app/photo/application/port/FolderRepositoryPort.kt
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
// src/main/kotlin/com/yapp2app/photo/infra/persist/FolderRepositoryAdapter.kt
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
// src/main/kotlin/com/yapp2app/photo/infra/persist/jpa/JpaFolderRepository.kt
interface JpaFolderRepository : JpaRepository<Folder, Long> {
    fun findAllByUserId(userId: Long): List<Folder>
    fun existsByUserIdAndName(userId: Long, name: String): Boolean
}
```

---

## Command/Result Pattern

### Command (Input)

```kotlin
// src/main/kotlin/com/yapp2app/photo/application/command/FolderCommand.kt
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
// src/main/kotlin/com/yapp2app/photo/application/result/FolderResult.kt
data class CreateFolderResult(
    val folderId: Long,
)

data class GetFoldersResult(
    val folders: List<FolderInfo>,
)
```

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

| Component | Location |
|-----------|----------|
| UseCase annotation | `src/main/kotlin/com/yapp2app/common/annotation/UseCase.kt` |
| Base entity | `src/main/kotlin/com/yapp2app/common/domain/BaseTimeEntity.kt` |
| Transaction runner | `src/main/kotlin/com/yapp2app/common/transaction/TransactionRunner.kt` |
