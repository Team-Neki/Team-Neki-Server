# API Development Patterns

Load this context when creating or modifying API endpoints.

---

## Response Wrapper

ALL API responses MUST use `BaseResponse<T>`:

```kotlin
data class BaseResponse<T>(
    val resultCode: String = ResultCode.SUCCESS.code,
    val message: String = ResultCode.SUCCESS.message,
    val data: T? = null,
)
```

**Usage in Controller:**
```kotlin
@GetMapping
fun getAllFolders(): BaseResponse<GetAllFolderResponse> {
    val result = getFoldersUseCase.execute(command)
    val response = resultConverter.toResponse(result)
    return BaseResponse(data = response)
}
```

Reference: `src/main/kotlin/com/yapp2app/common/api/dto/BaseResponse.kt`

---

## Data Flow Pattern

```
Request → Controller → Converter → Command → UseCase → Result → Converter → Response
```

### Complete Example

```kotlin
@RestController
@RequestMapping("/api/folders")
class FolderController(
    private val createFolderUseCase: CreateFolderUseCase,
    private val commandConverter: FolderCommandConverter,
    private val resultConverter: FolderResultConverter,
) {
    @PostMapping
    fun createFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: CreateFolderRequest,
    ): BaseResponse<CreateFolderResponse> {
        // 1. Convert request to command
        val command = commandConverter.toCreateFolderCommand(request, userId)

        // 2. Execute use case
        val result = createFolderUseCase.execute(command)

        // 3. Convert result to response
        val response = resultConverter.toCreateFolderResponse(result)

        // 4. Wrap in BaseResponse
        return BaseResponse(data = response)
    }
}
```

---

## Exception Handling

Use `BusinessException` with `ResultCode`:

```kotlin
// Throwing exceptions
throw BusinessException(ResultCode.CONFLICT_FOLDER)
throw BusinessException(ResultCode.NOT_FOUND)

// Available result codes
enum class ResultCode(val code: String, val message: String) {
    SUCCESS("D-0", "OK"),
    ERROR("D-99", "ERROR"),
    INVALID_PARAMETER("D-01", "Invalid input"),
    ALREADY_SIGNUP("D-02", "Already registered"),
    NOT_FOUND_USER("D-03", "User not found"),
    NOT_FOUND("D-04", "Data not found"),
    ALREADY_REQUEST("D-05", "Already processed"),
    CONFLICT_FOLDER("D-06", "Folder already exists"),
    // Token errors
    EXPIRED_TOKEN_ERROR("D-997", "Token expired"),
    INVALID_TOKEN_ERROR("D-998", "Invalid token"),
    SECURITY_ERROR("D-999", "Authentication failed"),
}
```

Key files:
- `src/main/kotlin/com/yapp2app/common/exception/BusinessException.kt`
- `src/main/kotlin/com/yapp2app/common/api/dto/ResultCode.kt`
- `src/main/kotlin/com/yapp2app/common/exception/handler/ExceptionHandler.kt`

---

## Swagger Documentation

Every endpoint MUST include `@Operation`:

```kotlin
@Operation(
    summary = "Create folder",           // Short description
    description = "Creates a new photo folder for the user."  // Detailed for frontend devs
)
@PostMapping
fun createFolder(...): BaseResponse<CreateFolderResponse>
```

### Multi-Step Workflow Documentation

For endpoints that are part of a workflow, document the full flow:

```kotlin
@Operation(
    summary = "Register photo image",
    description = """
        Photo upload workflow:
        1. Call GET /api/media/presigned-url → Get S3 upload URL
        2. Client uploads file directly to S3
        3. Call this API to register the photo metadata

        The imageKey from step 1 must be passed to this endpoint.
    """
)
```

### Security Annotation

Protected endpoints use `@RequiresSecurity`:

```kotlin
@RequiresSecurity  // Marks endpoint as requiring authentication in Swagger
@Tag(name = "folder", description = "Folder APIs")
@RestController
@RequestMapping("/api/folders")
class FolderController
```

Reference: `src/main/kotlin/com/yapp2app/common/api/document/SwaggerConfig.kt`

---

## Request Validation

Use Jakarta validation annotations:

```kotlin
data class CreateFolderRequest(
    @field:NotBlank(message = "Folder name is required")
    @field:Size(max = 50, message = "Folder name must be 50 characters or less")
    val name: String,
)
```

Validation errors are automatically handled and return:
```json
{
    "resultCode": "D-01",
    "message": "Folder name is required",
    "success": false,
    "errors": [
        { "field": "name", "message": "Folder name is required" }
    ]
}
```

---

## Authentication

Get current user ID from JWT:

```kotlin
@PostMapping
fun createFolder(
    @AuthenticationPrincipal(expression = "id") userId: Long,  // Extracts user ID from token
    @RequestBody request: CreateFolderRequest,
): BaseResponse<CreateFolderResponse>
```

---

## Checklist for New Endpoints

- [ ] Use `BaseResponse<T>` wrapper
- [ ] Create Request/Response DTOs in `api/dto/`
- [ ] Create Command in `application/command/`
- [ ] Create Result in `application/result/`
- [ ] Create Converters in `api/converter/`
- [ ] Add `@Operation` with summary and description
- [ ] Add `@RequiresSecurity` if authentication required
- [ ] Add validation annotations to request DTOs
- [ ] Write E2E tests (see `@.claude/docs/TESTING.md`)
