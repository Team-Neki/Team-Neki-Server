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

**Yapp** is a photo booth platform API server providing:
- **Photo Poses**: Users share photo booth pose recommendations
- **Photo Archiving**: Store and organize photos in folders
- **Booth Location Search**: Map-based search across multiple photo booth brands

| Profile | Infrastructure |
|---------|---------------|
| `local` | Docker Compose |
| `staging` | k3s (Linux) |
| `prod` | k3s (Linux) |

---

## Technology Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0, Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL (main), Redis (cache) |
| ORM | JPA + QueryDSL |
| Auth | JWT, OAuth (Kakao, Apple OIDC) |
| Storage | AWS S3 |
| Docs | SpringDoc OpenAPI (Swagger) |
| Testing | Kotest, MockK |
| Code Style | ktlint via Spotless |

---

## Pre-Task Checklist (Mandatory)

Before starting any task, load the relevant context document:

| Task Type | Required Document |
|-----------|-------------------|
| Writing tests | `@.claude/docs/TESTING.md` |
| API development | `@.claude/docs/API_PATTERNS.md` |
| Architecture/Design | `@.claude/docs/ARCHITECTURE.md` |
| Configuration | `@.claude/docs/CONFIGURATION.md` |
| Logging/Metrics | `@.claude/docs/OBSERVABILITY.md` |

---

## Critical Constraints

### ❌ NEVER DO

| Constraint | Reason |
|------------|--------|
| Import from other domains | Breaks module isolation |
| Bypass ports to access infra directly | Violates Clean Architecture (exception: auth/user) |
| Remove observability code | Critical for production debugging |

### ✅ ALWAYS DO

| Practice | Reason |
|----------|--------|
| Wrap responses in `BaseResponse` | Consistent API format |
| Use `BusinessException` for errors | Centralized error handling |
| Write E2E tests for new endpoints | Quality assurance |
| Follow existing package structure | Maintainability |
| Run `spotlessApply` before commit | Code style consistency |
| Delete dependent entities first | Prevents orphan records and FK violations |

---

## Code Modification Guidelines

1. **Minimal changes**: Prefer targeted fixes over large refactors
2. **Follow conventions**: Match existing naming and package patterns
3. **No new dependencies**: Unless explicitly requested
4. **No reformatting**: Only modify relevant files
5. **Test coverage**: Add E2E tests for new endpoints

---

## Context Loading Guide

Load additional context as needed using `@` references:

| Task | Load Command |
|------|--------------|
| Writing tests | `@.claude/docs/TESTING.md` |
| API development | `@.claude/docs/API_PATTERNS.md` |
| Architecture/Design | `@.claude/docs/ARCHITECTURE.md` |
| Configuration | `@.claude/docs/CONFIGURATION.md` |
| Logging/Metrics | `@.claude/docs/OBSERVABILITY.md` |

### Quick File Reference

| Component | Location |
|-----------|----------|
| UseCase annotation | `src/main/kotlin/com/yapp2app/common/annotation/UseCase.kt` |
| Base response | `src/main/kotlin/com/yapp2app/common/api/dto/BaseResponse.kt` |
| Result codes | `src/main/kotlin/com/yapp2app/common/api/dto/ResultCode.kt` |
| Business exception | `src/main/kotlin/com/yapp2app/common/exception/BusinessException.kt` |
| E2E test base | `src/test/kotlin/com/yapp2app/e2e/E2ETestBase.kt` |
