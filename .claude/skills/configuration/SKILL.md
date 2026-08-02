---
name: configuration
description: Load when working with environment settings, application profiles (local/staging/prod), Jasypt encryption, secrets management, or infrastructure configuration.
---

# Configuration Guide

Load this context when setting up environments, managing secrets, or modifying infrastructure.

---

## Environment Profiles

| Profile   | Infrastructure | Usage                  |
|-----------|----------------|------------------------|
| `local`   | Docker Compose | Local development      |
| `staging` | k3s (Linux)    | Pre-production testing |
| `prod`    | k3s (Linux)    | Production             |

### Local Development Setup

```bash
# Start infrastructure
docker compose up -d    # PostgreSQL + Redis + LocalStack S3

# Run application
./gradlew bootRun

# Stop databases
docker compose down
```

---

## Jasypt Encryption

Sensitive values are encrypted with Jasypt in 각 모듈의 `application-{module}.yaml`:

```yaml
spring:
  datasource:
    password: ENC(abcd1234encrypted...)
```

### Encrypting New Values

Use the test utility:

```kotlin
// apps/api/src/test/kotlin/com/neki/JasyptTest.kt
@Test
fun jasyptGeneratTest() {
    val text = "my_secret_value"
    val encrypted = jasyptStringEncryptor.encrypt(text)
    println("ENC($encrypted)")  // Use this in application.yml
}
```

### Jasypt Configuration

```kotlin
// modules/jasypt/src/main/kotlin/com/neki/config/jasypt/JasyptConfig.kt
@Configuration
class JasyptConfig {
    // Encryption settings:
    // - Algorithm: PBEWithHmacSHA512AndAES_256
    // - Password: From environment variable
}
```

---

## Configuration File Locations

| Type                   | Location                                                        |
|------------------------|-----------------------------------------------------------------|
| Application settings   | `apps/api/src/main/resources/application.yaml`          |
| Dependency settings    | `modules/{module}/src/main/resources/application-{module}.yaml` |
| Infrastructure configs | `modules/{module}/src/main/kotlin/com/neki/config/{module}/`    |
| Security configs       | `apps/api/src/main/kotlin/com/neki/user/infra/security/`                 |
| Swagger config         | `apps/api/src/main/kotlin/com/neki/common/api/document/SwaggerConfig.kt` |

---

## Infrastructure Configurations

### JPA & QueryDSL

```kotlin
// modules/postgres/src/main/kotlin/com/neki/config/postgres/JpaAuditingConfig.kt
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig

// modules/postgres/src/main/kotlin/com/neki/config/postgres/QueryDslConfig.kt
@Configuration
class QueryDslConfig {
    @Bean
    fun jpaQueryFactory(em: EntityManager) = JPAQueryFactory(em)
}
```

### Redis Cache

```kotlin
// modules/redis/src/main/kotlin/com/neki/config/redis/RedisCacheConfig.kt
@Configuration
@EnableCaching
class RedisCacheConfig {
    // Cache manager configuration
    // TTL settings
}
```

### REST Client

```kotlin
// apps/api/src/main/kotlin/com/neki/common/infra/config/RestClientConfig.kt
@Configuration
class RestClientConfig {
    // HTTP client for external APIs
}
```

---

## S3 Configuration

```kotlin
// modules/aws/src/main/kotlin/com/neki/config/aws/S3Properties.kt
@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucket: String,
    val endpoint: String? = null,  // LocalStack 여부를 가르는 신호. staging/prod 는 null
    val baseUrl: String = "",      // local 전용 (MediaTestController)
    val presignedUrlExpiration: Duration,
)

// modules/aws/src/main/kotlin/com/neki/config/aws/S3MediaStorageConfig.kt
@Configuration
class S3MediaStorageConfig {
    // S3 client bean configuration
}
```

---

## Security Configuration

### OAuth Properties

```kotlin
// apps/api/src/main/kotlin/com/neki/user/infra/security/config/OauthProperties.kt
@ConfigurationProperties(prefix = "oauth")
data class OauthProperties(
    val kakao: KakaoProperties,
    val apple: AppleProperties,
)
```

### JWT Settings

```kotlin
// apps/api/src/main/kotlin/com/neki/common/properties/AppProperties.kt
@ConfigurationProperties(prefix = "app")
class AppProperties(
    var version: String = "",
    var server: Server = Server(),
    var auth: Auth = Auth(),
    var cors: Cors = Cors(),   // S3 버킷 CORS 설정의 SSOT
)

class Auth(
    var accessTokenSecret: String? = null,
    var accessTokenExpiry: Long = 0,
    var refreshTokenSecret: String? = null,
    var refreshTokenExpiry: Long = 0,
)
```

---

## Terraform (Infrastructure as Code)

Staging infrastructure managed via Terraform:

```
infra/terraform/
├── aws/
│   ├── staging/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   └── modules/
│       └── s3/
│           └── README.md
└── CICD_SETUP.md
```

Reference: `infra/terraform/CICD_SETUP.md` for CI/CD setup guide.

---

## Adding New Configuration

1. 외부 의존성 설정이면 `modules/{module}/src/main/resources/application-{module}.yaml`, 애플리케이션 설정이면 `apps/api/src/main/resources/application.yaml` 에 추가 (민감값은 Jasypt 암호화)
2. Create `@ConfigurationProperties` class if complex
3. Inject via constructor in components
4. Document in this file
