---
name: api-scaffold
description: Scaffold a complete API endpoint with controller, use case, DTOs, converter, and E2E test
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
---

# API Endpoint Scaffolder

## 사전 준비

아래 Skill 파일을 읽고 컨벤션을 숙지하라:

- `.claude/skills/api-patterns/SKILL.md`
- `.claude/skills/architecture/SKILL.md`
- `.claude/skills/testing/SKILL.md`

## 입력 정보

사용자로부터 다음 정보를 파악한다:

- **도메인**: 패키지명 (예: `photo`, `pose`, `media`)
- **리소스**: 대상 엔티티 (예: `folder`, `photo-image`)
- **HTTP 메서드 + 경로**: (예: `POST /api/folders`)
- **기능 설명**: 엔드포인트가 하는 일
- **인증 필요 여부**: 기본값 yes

## 작업 절차

### 1. 기존 코드 분석

대상 도메인의 기존 파일을 읽어 컨벤션을 파악한다:

- `neki-application/src/main/kotlin/com/neki/{domain}/api/controller/` → Controller 스타일
- `neki-application/src/main/kotlin/com/neki/{domain}/api/dto/{Group}Converter.kt` → Converter 패턴
- `neki-application/src/main/kotlin/com/neki/{domain}/application/usecase/` → UseCase 패턴
- `neki-application/src/main/kotlin/com/neki/{domain}/api/dto/` → DTO 명명 패턴

### 2. 파일 생성

아래 7개 이상의 파일을 생성한다:

| # | 파일                              | 위치                                           |
|---|---------------------------------|----------------------------------------------|
| 1 | `{Action}{Resource}Request.kt`  | `neki-application/src/main/kotlin/com/neki/{domain}/api/dto/`                          |
| 2 | `{Action}{Resource}Response.kt` | `neki-application/src/main/kotlin/com/neki/{domain}/api/dto/`                          |
| 3 | `{Group}Command.kt` / `{Group}Query.kt` 내 중첩 클래스 | `neki-application/src/main/kotlin/com/neki/{domain}/application/dto/`                  |
| 4 | `{Group}Result.kt` 내 중첩 클래스                     | `neki-application/src/main/kotlin/com/neki/{domain}/application/dto/`                  |
| 5 | Converter 메서드                   | `neki-application/src/main/kotlin/com/neki/{domain}/api/dto/{Group}Converter.kt` (기존 파일에 추가 또는 신규)  |
| 6 | `{Action}{Resource}UseCase.kt`  | `neki-application/src/main/kotlin/com/neki/{domain}/application/usecase/`              |
| 7 | Controller 메서드                  | `neki-application/src/main/kotlin/com/neki/{domain}/api/controller/` (기존 파일에 추가 또는 신규) |
| 8 | `{Action}{Resource}E2ETest.kt`  | `neki-application/src/test/kotlin/com/neki/e2e/{domain}/`     |

### 3. 필수 적용 사항

- **BaseResponse 래핑**: 모든 응답은 `BaseResponse<T>`로 감싼다
- **@UseCase 어노테이션**: UseCase 클래스에 적용
- **@RequiresSecurity**: Controller 클래스에 적용
- **@Operation**: 모든 엔드포인트에 `summary`, `description` 포함
- **@AuthenticationPrincipal**: 인증 필요 시 `@AuthenticationPrincipal(expression = "id") userId: Long`
- **@Valid @RequestBody**: Request DTO에 Jakarta validation
- **명시적 타입 선언**: 메서드 호출 반환값에는 타입을 명시 (예: `val result: Type = useCase.execute(command)`)

### 4. 검증

- 생성된 파일이 기존 도메인 구조와 일관성이 있는지 확인한다.
- import 경로가 올바른지 확인한다.
