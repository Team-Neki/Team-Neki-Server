---
description: Kotlin 변수에 메서드 반환 타입을 명시하는 컨벤션 적용
allowed-tools: Read, Edit, Write, Glob, Grep, Bash, Task
---

Apply explicit variable type annotations to Kotlin source files following the project convention.

## Target

Apply to files specified by the user argument: $ARGUMENTS
If no argument is provided, ask which domain or file path to target.

## Convention Rules

Add explicit type annotation ONLY when assigning from **method/function call** return values:

| Pattern                   | Add Type | Example                                                       |
|---------------------------|----------|---------------------------------------------------------------|
| Method/function call      | YES      | `val result: GetTermsResult = getTermsUseCase.execute()`      |
| Chained method call       | YES      | `val terms: List<Term> = repo.findAll().filter { it.active }` |
| Static method call        | YES      | `val now: LocalDateTime = LocalDateTime.now()`                |
| Constructor call          | NO       | `val config = SimpleStringPBEConfig()`                        |
| Literal assignment        | NO       | `val name = "hello"`                                          |
| Collection builder        | NO       | `val list = listOf(1, 2, 3)`                                  |
| Type conversion           | NO       | `val num = id.toLong()`                                       |
| Property access           | NO       | `val uri = request.requestURI`                                |
| Destructuring             | NO       | `val (a, b) = pair`                                           |
| String template           | NO       | `val msg = "Hello $name"`                                     |
| Lambda/anonymous function | NO       | `val fn = { x: Int -> x + 1 }`                                |

## Common Framework Types

| Call Pattern                                       | Type            | Import                                    |
|----------------------------------------------------|-----------------|-------------------------------------------|
| `LoggerFactory.getLogger(javaClass)`               | `Logger`        | `org.slf4j.Logger`                        |
| `LocalDateTime.now()`                              | `LocalDateTime` | `java.time.LocalDateTime`                 |
| `objectMapper.readTree(...)`                       | `JsonNode`      | `com.fasterxml.jackson.databind.JsonNode` |
| `Jwts.parser()...parseSignedClaims(token).payload` | `Claims`        | `io.jsonwebtoken.Claims`                  |

## Procedure

1. **Read port interfaces** in the target domain's `application/port/` directory to identify return
   types
2. **Find all Kotlin files** in the target domain (UseCase, Adapter, Controller, Converter, Config,
   etc.)
3. **For each file**, identify `val name = methodCall(...)` patterns and add
   `val name: ReturnType = methodCall(...)`
4. **Add imports** if the type is not already imported
5. **Skip** variables that already have explicit types, and patterns listed as "NO" above
6. **Run** `./gradlew spotlessApply` after all changes to fix formatting
7. **Run** `./gradlew build` to verify compilation

## Domain Port Locations

| Domain  | Port Path                                                   |
|---------|-------------------------------------------------------------|
| term    | `src/main/kotlin/com/neki/domain/term/application/port/`    |
| version | `src/main/kotlin/com/neki/domain/version/application/port/` |
| user    | `src/main/kotlin/com/neki/domain/user/application/port/`    |
| auth    | `src/main/kotlin/com/neki/domain/auth/application/port/`    |
| map     | `src/main/kotlin/com/neki/domain/map/application/port/`     |
| pose    | `src/main/kotlin/com/neki/domain/pose/application/port/`    |
| photo   | `src/main/kotlin/com/neki/domain/photo/application/port/`   |
| media   | `src/main/kotlin/com/neki/domain/media/application/port/`   |
| common  | `src/main/kotlin/com/neki/common/`                          |
