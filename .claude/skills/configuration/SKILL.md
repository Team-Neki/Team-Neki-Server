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

**이 저장소는 AWS 인프라를 관리하지 않습니다.** Team-Neki-Platform 저장소의
`infra/` 가 계정의 IAM 과 S3 버킷을 통합 관리합니다.

이 앱이 쓰는 버킷도 그쪽에 있습니다.

| 버킷 | 용도 |
|---|---|
| `yapp-neki-ap-northeast-2` | prod 미디어 |
| `yapp-neki-staging-ap-northeast-2` | staging 미디어 |

예전에는 `infra/terraform/aws/staging` 이 staging 버킷을 관리했지만 state 가
로컬 파일이라 맥북 한 대에만 있었습니다. 잃으면 버킷이 고아가 되는 구조였습니다.
Platform 의 S3 백엔드로 옮겼습니다 (BACKEND-136).

버킷 설정을 바꾸려면 Platform 저장소에서 PR 을 올립니다. 여기서 `terraform` 을
다시 만들지 마세요. 같은 리소스를 두 state 가 관리하면 한쪽 apply 가 다른 쪽을
덮습니다.

---

## Adding New Configuration

1. 외부 의존성 설정이면 `modules/{module}/src/main/resources/application-{module}.yaml`, 애플리케이션 설정이면 `apps/api/src/main/resources/application.yaml` 에 추가 (민감값은 Jasypt 암호화)
2. Create `@ConfigurationProperties` class if complex
3. Inject via constructor in components
4. Document in this file
