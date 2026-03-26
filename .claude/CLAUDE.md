# CLAUDE.md

Instructions for Claude Code when working with this repository.

## Quick Reference

```bash
# Build & Run
./gradlew build                    # Build project
./gradlew bootRun                  # Run locally (requires Docker)
./gradlew test                     # Run all tests
./gradlew spotlessApply            # Format code (ktlint)

# Docker (local development)
docker compose up -d               # Start PostgreSQL + Redis
docker compose down                # Stop containers
```

---

## Project Overview

**Neki** is a photo booth platform API server providing:

- **Photo Poses**: Users share photo booth pose recommendations
- **Photo Archiving**: Store and organize photos in folders
- **Booth Location Search**: Map-based search across multiple photo booth brands

| Profile   | Infrastructure |
|-----------|----------------|
| `local`   | Docker Compose |
| `staging` | k3s (Linux)    |
| `prod`    | k3s (Linux)    |

---

## Technology Stack

| Category   | Technology                       |
|------------|----------------------------------|
| Language   | Kotlin 2.0, Java 21              |
| Framework  | Spring Boot 3.5                  |
| Database   | PostgreSQL (main), Redis (cache) |
| ORM        | JPA + QueryDSL                   |
| Auth       | JWT, OAuth (Kakao, Apple OIDC)   |
| Storage    | AWS S3                           |
| Docs       | SpringDoc OpenAPI (Swagger)      |
| Testing    | Kotest, MockK                    |
| Code Style | ktlint via Spotless              |

---

## Pre-Task Checklist (Mandatory)

Before starting any task, load the relevant skill (auto-loaded based on context, or invoke via slash
command):

| Task Type           | Skill (slash command) |
|---------------------|-----------------------|
| Writing tests       | `/testing`            |
| API development     | `/api-patterns`       |
| Architecture/Design | `/architecture`       |
| Configuration       | `/configuration`      |
| Logging/Metrics     | `/observability`      |
| S3 uploads/Media    | `/presigned-url-flow` |
| Committing changes  | `/commit`             |

---

## Critical Constraints

### ❌ NEVER DO

| Constraint                            | Reason                                        |
|---------------------------------------|-----------------------------------------------|
| Import from other domains             | Breaks module isolation                       |
| Bypass ports to access infra directly | Violates Clean Architecture (exception: user) |
| Remove observability code             | Critical for production debugging             |

### ✅ ALWAYS DO

| Practice                                     | Reason                                                                                           |
|----------------------------------------------|--------------------------------------------------------------------------------------------------|
| Wrap responses in `BaseResponse`             | Consistent API format                                                                            |
| Use `BusinessException` for errors           | Centralized error handling                                                                       |
| Write E2E tests for new endpoints            | Quality assurance                                                                                |
| Follow existing package structure            | Maintainability                                                                                  |
| Run `spotlessApply` before commit            | Code style consistency                                                                           |
| Delete dependent entities first              | Prevents orphan records and FK violations                                                        |
| Add Flyway migration when changing DB schema | `@Column` length, type, constraint 변경 시 `src/main/resources/db/migration/` 에 다음 버전의 SQL 파일 추가 필수 |

---

## Code Modification Guidelines

1. **Minimal changes**: Prefer targeted fixes over large refactors
2. **Follow conventions**: Match existing naming and package patterns
3. **No new dependencies**: Unless explicitly requested
4. **No reformatting**: Only modify relevant files
5. **Test coverage**: Add E2E tests for new endpoints

### Variable Type Declaration Convention

- **Explicit type**: When assigning from method/function calls → `val result: Type = someMethod()`
- **Type inference**: For constructors, literals, collection builders, type conversions, property
  access
- Reference: `.claude/commands/add-types.md` for full rules

---

## Skills (Auto-loaded Context)

Skills are auto-loaded when relevant tasks are detected, or can be invoked manually:

| Skill                | Slash Command         | Auto-loads When                                   |
|----------------------|-----------------------|---------------------------------------------------|
| `api-patterns`       | `/api-patterns`       | API endpoint development, request/response design |
| `architecture`       | `/architecture`       | New domain/module design, Clean Architecture      |
| `configuration`      | `/configuration`      | Environment settings, profiles, secrets           |
| `observability`      | `/observability`      | Logging, metrics, monitoring                      |
| `testing`            | `/testing`            | Writing tests, test coverage                      |
| `commit`             | `/commit`             | 코드 작업 완료 후 커밋 생성                                  |
| `presigned-url-flow` | `/presigned-url-flow` | S3 upload, media/image handling                   |

### Quick File Reference

| Component          | Location                                                         |
|--------------------|------------------------------------------------------------------|
| UseCase annotation | `src/main/kotlin/com/neki/common/annotation/UseCase.kt`          |
| Base response      | `src/main/kotlin/com/neki/common/api/dto/BaseResponse.kt`        |
| Result codes       | `src/main/kotlin/com/neki/common/api/dto/ResultCode.kt`          |
| Business exception | `src/main/kotlin/com/neki/common/exception/BusinessException.kt` |
| E2E test base      | `src/test/kotlin/com/neki/e2e/E2ETestBase.kt`                    |
