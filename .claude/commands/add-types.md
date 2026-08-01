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

Port 인터페이스는 모두 `:neki-application` 모듈에 있다. `support`가 약관·앱버전을, `user`가 인증을 함께 담당한다.

| Domain       | Port Path                                                                     |
|--------------|-------------------------------------------------------------------------------|
| map          | `neki-application/src/main/kotlin/com/neki/map/application/port/`             |
| media        | `neki-application/src/main/kotlin/com/neki/media/application/port/`           |
| notification | `neki-application/src/main/kotlin/com/neki/notification/application/port/`    |
| photo        | `neki-application/src/main/kotlin/com/neki/photo/application/port/`           |
| pose         | `neki-application/src/main/kotlin/com/neki/pose/application/port/`            |
| support      | `neki-application/src/main/kotlin/com/neki/support/application/port/`         |
| user         | `neki-application/src/main/kotlin/com/neki/user/application/port/`            |
| common (공유 커널) | `neki-core/src/main/kotlin/com/neki/common/`                              |
