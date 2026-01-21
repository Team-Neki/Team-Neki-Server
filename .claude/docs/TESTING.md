# Testing Guide

Load this context when writing or modifying tests.

---

## Test Strategy

| Type | When to Write | Coverage Goal |
|------|---------------|---------------|
| **E2E** | Required for ALL API endpoints | 100% endpoint coverage |
| **Unit** | Complex logic, exception-heavy code | Selective |

**Philosophy**: Prefer E2E tests over unit tests. Unit tests only when logic is complex enough to warrant isolated testing.

---

## E2E Test Structure

### Base Classes

```kotlin
// All E2E tests extend from E2ETestBase
abstract class E2ETestBase {
    @Autowired
    protected lateinit var tokenProvider: AuthTokenProvider

    @Autowired
    protected lateinit var userRepository: UserRepository

    @AfterEach
    protected open fun tearDown() {
        userRepository.deleteAllInBatch()
    }

    // Create test user and get JWT token
    fun createTestUserAndToken(
        email: String = "test-${System.currentTimeMillis()}@example.com",
        name: String = "Test User",
        providerType: ProviderType = ProviderType.TEST,
    ): Pair<User, String>
}
```

Reference: `src/test/kotlin/com/yapp2app/e2e/E2ETestBase.kt`

### Domain-Specific Base Classes

For domain-specific setup, extend the domain base class:

```kotlin
// Photo domain tests
class CreateFolderE2ETest : FolderE2ETestBase() {
    // FolderE2ETestBase extends E2ETestBase
    // Adds folder-specific setup/teardown
}
```

---

## Test Directory Structure

```
src/test/kotlin/com/yapp2app/
├── e2e/                          # E2E tests (organized by domain)
│   ├── E2ETestBase.kt           # Base class for all E2E tests
│   ├── auth/
│   │   └── AuthE2ETest.kt
│   ├── photo/
│   │   └── folder/
│   │       ├── FolderE2ETestBase.kt
│   │       ├── CreateFolderE2ETest.kt
│   │       ├── DeleteFolderE2ETest.kt
│   │       ├── GetAllFolderE2ETest.kt
│   │       └── UpdateFolderE2ETest.kt
│   └── user/
│       └── UserE2ETest.kt
├── auth/                         # Unit tests (next to domain)
│   └── infra/security/filter/
│       └── AuthMdcFilterTest.kt
├── common/
│   └── filter/
│       └── RequestMdcFilterTest.kt
└── JasyptTest.kt                # Utility tests
```

---

## Test Patterns

### Creating Authenticated Requests

```kotlin
@Test
fun `should create folder successfully`() {
    // 1. Create test user and get token
    val (user, token) = createTestUserAndToken()

    // 2. Make authenticated request
    RestAssured.given()
        .header("Authorization", "Bearer $token")
        .contentType(ContentType.JSON)
        .body(CreateFolderRequest(name = "My Folder"))
        .`when`()
        .post("/api/folders")
        .then()
        .statusCode(200)
        .body("success", equalTo(true))
        .body("data.folderId", notNullValue())
}
```

### Testing Error Cases

```kotlin
@Test
fun `should return error when folder name duplicated`() {
    val (user, token) = createTestUserAndToken()

    // Create first folder
    createFolder(token, "Duplicate Name")

    // Try to create duplicate
    RestAssured.given()
        .header("Authorization", "Bearer $token")
        .contentType(ContentType.JSON)
        .body(CreateFolderRequest(name = "Duplicate Name"))
        .`when`()
        .post("/api/folders")
        .then()
        .statusCode(400)
        .body("resultCode", equalTo("D-06"))  // CONFLICT_FOLDER
}
```

---

## Test Profile

Tests run with `@ActiveProfiles("test")`:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MyE2ETest : E2ETestBase() {
    // ...
}
```

---

## Testing Tools

| Tool | Purpose |
|------|---------|
| **Kotest** | Test framework with DSL |
| **MockK** | Mocking library for Kotlin |
| **RestAssured** | HTTP testing |
| **Testcontainers** | Database containers |

---

## Jasypt Test Utility

Encrypt sensitive values for configuration:

```kotlin
// src/test/kotlin/com/yapp2app/JasyptTest.kt
@Test
fun jasyptGeneratTest() {
    val text = "value_to_encrypt"
    val encrypted = jasyptStringEncryptor.encrypt(text)
    println("ENC($encrypted)")
}
```

---

## Checklist for New Endpoints

- [ ] Create E2E test class extending appropriate base
- [ ] Test happy path (success case)
- [ ] Test authentication required (401 without token)
- [ ] Test validation errors (400 with invalid input)
- [ ] Test business rule violations (appropriate error codes)
- [ ] Clean up test data in `@AfterEach`
