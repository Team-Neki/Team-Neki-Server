# Configuration Guide

Load this context when setting up environments, managing secrets, or modifying infrastructure.

---

## Environment Profiles

| Profile | Infrastructure | Usage |
|---------|---------------|-------|
| `local` | Docker Compose | Local development |
| `staging` | k3s (Linux) | Pre-production testing |
| `prod` | k3s (Linux) | Production |

### Local Development Setup

```bash
# Start databases
docker compose up -d    # PostgreSQL + Redis

# Run application
./gradlew bootRun

# Stop databases
docker compose down
```

---

## Jasypt Encryption

Sensitive values are encrypted with Jasypt in `application.yml`:

```yaml
spring:
  datasource:
    password: ENC(abcd1234encrypted...)
```

### Encrypting New Values

Use the test utility:

```kotlin
// src/test/kotlin/com/yapp2app/JasyptTest.kt
@Test
fun jasyptGeneratTest() {
    val text = "my_secret_value"
    val encrypted = jasyptStringEncryptor.encrypt(text)
    println("ENC($encrypted)")  // Use this in application.yml
}
```

### Jasypt Configuration

```kotlin
// src/main/kotlin/com/yapp2app/common/config/JasyptConfig.kt
@Configuration
class JasyptConfig {
    // Encryption settings:
    // - Algorithm: PBEWithHmacSHA512AndAES_256
    // - Password: From environment variable
}
```

---

## Configuration File Locations

| Type | Location |
|------|----------|
| Application settings | `src/main/resources/application.yml` |
| Profile-specific | `src/main/resources/application-{profile}.yml` |
| Infrastructure configs | `src/main/kotlin/com/yapp2app/common/infra/config/` |
| Security configs | `src/main/kotlin/com/yapp2app/auth/infra/security/` |
| Swagger config | `src/main/kotlin/com/yapp2app/common/api/document/SwaggerConfig.kt` |

---

## Infrastructure Configurations

### JPA & QueryDSL

```kotlin
// src/main/kotlin/com/yapp2app/common/infra/config/JpaAuditingConfig.kt
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig

// src/main/kotlin/com/yapp2app/common/infra/config/QueryDslConfig.kt
@Configuration
class QueryDslConfig {
    @Bean
    fun jpaQueryFactory(em: EntityManager) = JPAQueryFactory(em)
}
```

### Redis Cache

```kotlin
// src/main/kotlin/com/yapp2app/common/config/RedisCacheConfig.kt
@Configuration
@EnableCaching
class RedisCacheConfig {
    // Cache manager configuration
    // TTL settings
}
```

### REST Client

```kotlin
// src/main/kotlin/com/yapp2app/common/infra/config/RestClientConfig.kt
@Configuration
class RestClientConfig {
    // HTTP client for external APIs
}
```

---

## S3 Configuration

```kotlin
// src/main/kotlin/com/yapp2app/media/infra/s3/S3Properties.kt
@ConfigurationProperties(prefix = "cloud.aws.s3")
data class S3Properties(
    val bucket: String,
    val region: String,
)

// src/main/kotlin/com/yapp2app/media/infra/s3/S3MediaStorageConfig.kt
@Configuration
class S3MediaStorageConfig {
    // S3 client bean configuration
}
```

---

## Security Configuration

### OAuth Properties

```kotlin
// src/main/kotlin/com/yapp2app/auth/infra/security/properties/OauthProperties.kt
@ConfigurationProperties(prefix = "oauth")
data class OauthProperties(
    val kakao: KakaoProperties,
    val apple: AppleProperties,
)
```

### JWT Settings

```kotlin
// src/main/kotlin/com/yapp2app/auth/infra/security/properties/AppProperties.kt
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val auth: AuthProperties,
)

data class AuthProperties(
    val tokenSecret: String,
    val tokenExpiry: Long,
    val refreshTokenExpiry: Long,
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

1. Add property to `application.yml` (encrypt sensitive values)
2. Create `@ConfigurationProperties` class if complex
3. Inject via constructor in components
4. Document in this file
