# Admin Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add email/password login + JWT issuance to `apps/admin`, and protect the existing Brand/Pose admin endpoints with the issued token.

**Architecture:** Reuse the existing `User` entity (`RoleType.ADMIN`) and the existing JWT stack in `domain.user.infra.security.*` (`AuthTokenProviderAdapter`, `JwtAuthenticationFilter`, `CustomAuthenticationEntryPoint`, `CustomAccessDeniedHandler`, `AuthService`). `apps/admin` wires these as explicit `@Bean`s (same pattern as the existing `DomainServiceConfig`) instead of component-scanning `domain.user.infra.security.config`, because that package also holds the `apps/api`-specific `SecurityConfig` and OAuth beans admin doesn't need.

**Tech Stack:** Spring Security (filter chain + `BCryptPasswordEncoder`), jjwt 0.12.5, Kotest + MockK for the new domain-service test.

**Full design:** `docs/superpowers/specs/2026-08-16-admin-auth-design.md`

---

## Task 1: Add `ResultCode.INVALID_CREDENTIALS`

**Files:**
- Modify: `core/src/main/kotlin/com/neki/core/code/ResultCode.kt`

- [ ] **Step 1: Add the new enum entry**

In `core/src/main/kotlin/com/neki/core/code/ResultCode.kt`, add a new entry right after `PUSH_SEND_FAILED`:

```kotlin
    PUSH_SEND_FAILED("D-12", "푸시 알림 발송에 실패했습니다."),
    INVALID_CREDENTIALS("D-13", "이메일 또는 비밀번호가 올바르지 않습니다."),
```

- [ ] **Step 2: Compile to confirm no syntax errors**

Run: `./gradlew :core:compileKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/neki/core/code/ResultCode.kt
git commit -m "feat: 이메일/비밀번호 로그인 실패용 ResultCode 추가"
```

---

## Task 2: `UserRepository.findByEmail`

**Files:**
- Modify: `domain/src/main/kotlin/com/neki/domain/user/repository/UserRepository.kt`
- Modify: `domain/src/main/kotlin/com/neki/domain/user/infra/persist/jpa/JpaUserRepository.kt`
- Modify: `domain/src/main/kotlin/com/neki/domain/user/infra/persist/UserRepositoryAdapter.kt`

- [ ] **Step 1: Add `findByEmail` to the domain interface**

In `domain/src/main/kotlin/com/neki/domain/user/repository/UserRepository.kt`, the file currently reads:

```kotlin
interface UserRepository {
    fun save(user: User): User

    fun findByOid(oid: String, provider: ProviderType): User?

    fun findById(id: Long): User?

    fun countByOidIsNotNull(): Long
}
```

Change it to:

```kotlin
interface UserRepository {
    fun save(user: User): User

    fun findByOid(oid: String, provider: ProviderType): User?

    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun countByOidIsNotNull(): Long
}
```

- [ ] **Step 2: Add the derived query to the JPA repository**

In `domain/src/main/kotlin/com/neki/domain/user/infra/persist/jpa/JpaUserRepository.kt`, the file currently reads:

```kotlin
interface JpaUserRepository : JpaRepository<User, Long> {

    fun existsByName(name: String): Boolean

    fun findByOidAndProviderType(oid: String, providerType: ProviderType): User?

    fun countByOidIsNotNull(): Long
}
```

Change it to:

```kotlin
interface JpaUserRepository : JpaRepository<User, Long> {

    fun existsByName(name: String): Boolean

    fun findByOidAndProviderType(oid: String, providerType: ProviderType): User?

    fun findByEmail(email: String): User?

    fun countByOidIsNotNull(): Long
}
```

- [ ] **Step 3: Delegate from the adapter**

In `domain/src/main/kotlin/com/neki/domain/user/infra/persist/UserRepositoryAdapter.kt`, the file currently reads:

```kotlin
@Repository
class UserRepositoryAdapter(private val jpaRepository: JpaUserRepository) : UserRepository {

    override fun save(user: User): User = jpaRepository.save(user)

    override fun findByOid(oid: String, providerType: ProviderType): User? =
        jpaRepository.findByOidAndProviderType(oid, providerType)

    override fun findById(id: Long): User? = jpaRepository.findByIdOrNull(id)

    override fun countByOidIsNotNull(): Long = jpaRepository.countByOidIsNotNull()
}
```

Change it to:

```kotlin
@Repository
class UserRepositoryAdapter(private val jpaRepository: JpaUserRepository) : UserRepository {

    override fun save(user: User): User = jpaRepository.save(user)

    override fun findByOid(oid: String, providerType: ProviderType): User? =
        jpaRepository.findByOidAndProviderType(oid, providerType)

    override fun findById(id: Long): User? = jpaRepository.findByIdOrNull(id)

    override fun findByEmail(email: String): User? = jpaRepository.findByEmail(email)

    override fun countByOidIsNotNull(): Long = jpaRepository.countByOidIsNotNull()
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :domain:compileKotlin -q`
Expected: no output, exit code 0. (Compiles clean since every implementer of `UserRepository` was just updated — there are no other implementers besides `UserRepositoryAdapter`; a mock-based test would only break if it used `relaxUnitFun`-less strict mocking of the full interface, which none currently do.)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/neki/domain/user/repository/UserRepository.kt \
  domain/src/main/kotlin/com/neki/domain/user/infra/persist/jpa/JpaUserRepository.kt \
  domain/src/main/kotlin/com/neki/domain/user/infra/persist/UserRepositoryAdapter.kt
git commit -m "feat: UserRepository에 findByEmail 추가"
```

---

## Task 3: `AuthService.authenticateByEmail` (TDD)

**Files:**
- Modify: `domain/src/main/kotlin/com/neki/domain/user/service/AuthService.kt`
- Create: `domain/src/test/kotlin/com/neki/domain/user/service/AuthServiceTest.kt`
- Modify: `apps/api/src/test/kotlin/com/neki/api/user/application/usecase/RefreshTokenUseCaseTest.kt`
- Modify: `apps/api/src/test/kotlin/com/neki/api/user/application/usecase/OauthLoginUseCaseTest.kt`

This adds two new constructor parameters to `AuthService` (`passwordEncoder`, `userRepository`). Two existing apps/api tests construct `AuthService(...)` directly and must be updated in the same commit or they won't compile.

- [ ] **Step 1: Write the failing test**

Create `domain/src/test/kotlin/com/neki/domain/user/service/AuthServiceTest.kt`:

```kotlin
package com.neki.domain.user.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.user.external.AuthTokenProvider
import com.neki.domain.user.external.OidcTokenValidator
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest : FunSpec({

    lateinit var tokenProviderPort: AuthTokenProvider
    lateinit var oidcTokenValidatorPort: OidcTokenValidator
    lateinit var passwordEncoder: PasswordEncoder
    lateinit var userRepository: UserRepository
    lateinit var authService: AuthService

    fun aUser(roles: String, password: String = "hashed"): User = User(
        email = "admin@neki.com",
        password = password,
        oid = null,
        name = "admin",
        providerType = ProviderType.LOCAL,
        profileImageId = null,
        roles = roles,
    )

    beforeTest {
        tokenProviderPort = mockk()
        oidcTokenValidatorPort = mockk()
        passwordEncoder = mockk()
        userRepository = mockk()
        authService = AuthService(tokenProviderPort, oidcTokenValidatorPort, passwordEncoder, userRepository)
    }

    test("정상 - 이메일/비밀번호 일치 + ROLE_ADMIN 보유 시 User 반환") {
        val user = aUser(roles = "${RoleType.USER.role},${RoleType.ADMIN.role}")
        every { userRepository.findByEmail("admin@neki.com") } returns user
        every { passwordEncoder.matches("raw-password", "hashed") } returns true

        val result = authService.authenticateByEmail("admin@neki.com", "raw-password")

        result shouldBe user
    }

    test("실패 - 이메일로 사용자를 찾을 수 없으면 INVALID_CREDENTIALS") {
        every { userRepository.findByEmail("nobody@neki.com") } returns null

        val ex = shouldThrow<BusinessException> {
            authService.authenticateByEmail("nobody@neki.com", "raw-password")
        }
        ex.resultCode shouldBe ResultCode.INVALID_CREDENTIALS
    }

    test("실패 - 비밀번호가 틀리면 INVALID_CREDENTIALS") {
        val user = aUser(roles = "${RoleType.USER.role},${RoleType.ADMIN.role}")
        every { userRepository.findByEmail("admin@neki.com") } returns user
        every { passwordEncoder.matches("wrong-password", "hashed") } returns false

        val ex = shouldThrow<BusinessException> {
            authService.authenticateByEmail("admin@neki.com", "wrong-password")
        }
        ex.resultCode shouldBe ResultCode.INVALID_CREDENTIALS
    }

    test("실패 - ROLE_ADMIN이 없으면 INVALID_CREDENTIALS") {
        val user = aUser(roles = RoleType.USER.role)
        every { userRepository.findByEmail("user@neki.com") } returns user
        every { passwordEncoder.matches("raw-password", "hashed") } returns true

        val ex = shouldThrow<BusinessException> {
            authService.authenticateByEmail("user@neki.com", "raw-password")
        }
        ex.resultCode shouldBe ResultCode.INVALID_CREDENTIALS
    }
})
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :domain:compileTestKotlin -q`
Expected: FAIL — `AuthService` has no method `authenticateByEmail` and its constructor doesn't accept 4 arguments yet.

- [ ] **Step 3: Update `AuthService`**

In `domain/src/main/kotlin/com/neki/domain/user/service/AuthService.kt`, the file currently reads:

```kotlin
package com.neki.domain.user.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.user.dto.AuthCommand
import com.neki.domain.user.external.AuthTokenProvider
import com.neki.domain.user.external.OidcTokenValidator
import com.neki.domain.user.models.IssuedTokens
import com.neki.domain.user.models.OauthUserInfo
import com.neki.domain.user.models.TokenPrincipal
import com.neki.domain.user.models.User
import org.springframework.stereotype.Component

/**
 * fileName       : AuthService
 * author         : koo
 * date           : 2026. 8. 3. 오전 2:03
 * description    : 인증 토큰 도메인 서비스
 */
@Component
class AuthService(
    private val tokenProviderPort: AuthTokenProvider,
    private val oidcTokenValidatorPort: OidcTokenValidator,
) {

    /**
     * OIDC idToken 검증 후 OAuth 사용자 정보 추출
     */
    fun validateOauthToken(command: AuthCommand.RegisterOauthUser): OauthUserInfo =
        oidcTokenValidatorPort.validateIdToken(command.idToken, command.providerType, command.platform)
```

Change the imports and class declaration to:

```kotlin
package com.neki.domain.user.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.user.dto.AuthCommand
import com.neki.domain.user.external.AuthTokenProvider
import com.neki.domain.user.external.OidcTokenValidator
import com.neki.domain.user.models.IssuedTokens
import com.neki.domain.user.models.OauthUserInfo
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.TokenPrincipal
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * fileName       : AuthService
 * author         : koo
 * date           : 2026. 8. 3. 오전 2:03
 * description    : 인증 토큰 도메인 서비스
 */
@Component
class AuthService(
    private val tokenProviderPort: AuthTokenProvider,
    private val oidcTokenValidatorPort: OidcTokenValidator,
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
) {

    /**
     * OIDC idToken 검증 후 OAuth 사용자 정보 추출
     */
    fun validateOauthToken(command: AuthCommand.RegisterOauthUser): OauthUserInfo =
        oidcTokenValidatorPort.validateIdToken(command.idToken, command.providerType, command.platform)

    /**
     * 이메일/비밀번호 로그인. 이메일이 없거나, 비밀번호가 틀리거나, ROLE_ADMIN이 없으면
     * 전부 같은 INVALID_CREDENTIALS로 실패시켜 이메일 존재 여부가 응답으로 드러나지 않게 한다.
     */
    fun authenticateByEmail(email: String, rawPassword: String): User {
        val user: User = userRepository.findByEmail(email)
            ?: throw BusinessException(ResultCode.INVALID_CREDENTIALS)

        if (!passwordEncoder.matches(rawPassword, user.password)) {
            throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        }

        if (!user.roles.split(",").contains(RoleType.ADMIN.role)) {
            throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        }

        return user
    }
```

Leave `issueTokens` and `rotateTokens` (the rest of the file) unchanged.

- [ ] **Step 4: Run the new test**

Run: `./gradlew :domain:test --tests "com.neki.domain.user.service.AuthServiceTest" -q`
Expected: 4 tests pass.

- [ ] **Step 5: Fix the two apps/api call sites that construct `AuthService` directly**

In `apps/api/src/test/kotlin/com/neki/api/user/application/usecase/RefreshTokenUseCaseTest.kt`, find:

```kotlin
        useCase = RefreshTokenUseCase(AuthService(tokenProviderPort, mockk()))
```

Replace with:

```kotlin
        useCase = RefreshTokenUseCase(AuthService(tokenProviderPort, mockk(), mockk(), mockk()))
```

In `apps/api/src/test/kotlin/com/neki/api/user/application/usecase/OauthLoginUseCaseTest.kt`, find:

```kotlin
            authService = AuthService(tokenProviderPort, oidcTokenValidatorPort),
```

Replace with:

```kotlin
            authService = AuthService(tokenProviderPort, oidcTokenValidatorPort, mockk(), mockk()),
```

- [ ] **Step 6: Run full apps/api + domain test suites**

Run: `./gradlew :domain:test :apps:api:test -q`
Expected: BUILD SUCCESSFUL, no failures.

- [ ] **Step 7: Commit**

```bash
git add domain/src/main/kotlin/com/neki/domain/user/service/AuthService.kt \
  domain/src/test/kotlin/com/neki/domain/user/service/AuthServiceTest.kt \
  apps/api/src/test/kotlin/com/neki/api/user/application/usecase/RefreshTokenUseCaseTest.kt \
  apps/api/src/test/kotlin/com/neki/api/user/application/usecase/OauthLoginUseCaseTest.kt
git commit -m "feat: AuthService에 이메일/비밀번호 인증 추가"
```

---

## Task 4: `apps/admin` dependencies (Spring Security + JWT)

**Files:**
- Modify: `apps/admin/build.gradle.kts`

- [ ] **Step 1: Add dependencies**

In `apps/admin/build.gradle.kts`, the file currently reads:

```kotlin
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // :domain 이 두 의존성을 implementation 으로 잡아 전이되지 않는다.
    // modules:postgres 대신 직접 선언해 H2 로 띄우는 현재 구성을 유지한다.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")

    // 당분간 H2 로 운영한다. PostgreSQL 전환 시 modules:postgres 로 교체한다
    runtimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
```

Change it to:

```kotlin
plugins {
    id("org.springframework.boot")
}

val jwtVersion = "0.12.5"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // :domain 이 두 의존성을 implementation 으로 잡아 전이되지 않는다.
    // modules:postgres 대신 직접 선언해 H2 로 띄우는 현재 구성을 유지한다.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")

    // 이메일/비밀번호 로그인 + JWT 발급/검증
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")

    // 당분간 H2 로 운영한다. PostgreSQL 전환 시 modules:postgres 로 교체한다
    runtimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
```

- [ ] **Step 2: Verify the dependency resolves**

Run: `./gradlew :apps:admin:dependencies --configuration compileClasspath -q | grep jjwt`
Expected: three lines listing `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` at `0.12.5`.

- [ ] **Step 3: Commit**

```bash
git add apps/admin/build.gradle.kts
git commit -m "chore: admin 모듈에 spring-security, jjwt 의존성 추가"
```

---

## Task 5: `AdminApplication` scan config

**Files:**
- Modify: `apps/admin/src/main/kotlin/com/neki/admin/AdminApplication.kt`

- [ ] **Step 1: Update the application class**

The file currently reads:

```kotlin
package com.neki.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// admin 이 쓰는 도메인 서비스는 DomainServiceConfig 에 명시적으로 등록한다.
@SpringBootApplication(scanBasePackages = ["com.neki.admin", "com.neki.core"])
// 엔티티는 :domain·:core 에 있고, JPA 리포지터리는 admin 의 infra 에만 둔다.
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.admin")
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
```

Change it to:

```kotlin
package com.neki.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// admin 이 쓰는 도메인 서비스는 DomainServiceConfig 에 명시적으로 등록한다.
@SpringBootApplication(scanBasePackages = ["com.neki.admin", "com.neki.core"])
// AppProperties(app.auth.* 등)를 빈으로 바인딩한다.
@ConfigurationPropertiesScan("com.neki.core")
// 엔티티는 :domain·:core 에 있고, JPA 리포지터리는 admin 의 infra 에만 둔다.
@EntityScan("com.neki.domain", "com.neki.core")
// User 는 domain 소유라 JpaUserRepository 도 domain 에 있다 — 로그인에만 필요해 이 한 패키지만 추가로 연다.
@EnableJpaRepositories("com.neki.admin", "com.neki.domain.user.infra.persist.jpa")
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :apps:admin:compileKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add apps/admin/src/main/kotlin/com/neki/admin/AdminApplication.kt
git commit -m "feat: AdminApplication에 AppProperties 바인딩과 User JPA 리포지터리 스캔 추가"
```

---

## Task 6: JWT secrets in `apps/admin` config

**Files:**
- Modify: `apps/admin/src/main/resources/application.yaml`
- Modify: `apps/admin/src/test/resources/application-test.yml`

- [ ] **Step 1: Add `app.auth` to the local profile**

In `apps/admin/src/main/resources/application.yaml`, the file currently reads:

```yaml
spring:
  application:
    name: admin
  profiles:
    active: local

server:
  port: 8081

---
# 당분간 H2 로 띄운다. PostgreSQL 연결(modules:postgres, 프로파일 분리)은 후속 작업이다.
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:h2:mem:admindb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    open-in-view: false
  h2:
    console:
      enabled: true
  flyway:
    enabled: false
```

Change it to:

```yaml
spring:
  application:
    name: admin
  profiles:
    active: local

server:
  port: 8081

---
# 당분간 H2 로 띄운다. PostgreSQL 연결(modules:postgres, 프로파일 분리)은 후속 작업이다.
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:h2:mem:admindb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    # data-local.sql이 스키마 생성 이후에 실행되도록 미룬다.
    defer-datasource-initialization: true
    open-in-view: false
  sql:
    init:
      mode: always
      data-locations: classpath:data-local.sql
  h2:
    console:
      enabled: true
  flyway:
    enabled: false

# 로컬 전용 평문 시크릿. Jasypt 미사용 (apps/api 테스트 프로필과 동일한 방식).
app:
  auth:
    accessTokenSecret: adminLocalAccessTokenSecretMustBeVeryLongDoesNotUseJasypt
    accessTokenExpiry: 3600000 #1시간
    refreshTokenSecret: adminLocalRefreshTokenSecretMustBeVeryLongDoesNotUseJasypt
    refreshTokenExpiry: 604800000 #7일
```

- [ ] **Step 2: Add `app.auth` to the test profile**

In `apps/admin/src/test/resources/application-test.yml`, the file currently reads:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:admintestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    open-in-view: false
  flyway:
    enabled: false
```

Change it to:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:admintestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    open-in-view: false
  flyway:
    enabled: false

app:
  auth:
    accessTokenSecret: testSecretTokenMustBeVeryLongTestSecretDoesNotUseJasypt
    accessTokenExpiry: 3600000 #1시간
    refreshTokenSecret: testrefreshTokenMustBeVeryLongTestSecretDoesNotUseJasypt
    refreshTokenExpiry: 604800000 #7일
```

- [ ] **Step 3: Commit**

```bash
git add apps/admin/src/main/resources/application.yaml apps/admin/src/test/resources/application-test.yml
git commit -m "chore: admin local/test 프로필에 JWT 시크릿 추가"
```

(Task 6 references `data-local.sql`, which doesn't exist until Task 10. That's fine — no task before Task 10 boots the app, only `compileKotlin`, which doesn't touch resource files. `:apps:admin:bootRun` is first run in Task 10 Step 3, by which point the file exists.)

---

## Task 7: Admin `ExceptionHandler`

**Files:**
- Create: `apps/admin/src/main/kotlin/com/neki/admin/config/ExceptionHandler.kt`

`apps/admin` currently has no `@RestControllerAdvice` at all — `BusinessException` (already thrown by `BrandService`/`PoseService` today) would otherwise surface as an unhandled 500. This is required for the login endpoint's `INVALID_CREDENTIALS` failure to return a clean response, and fixes the same latent gap for the existing Brand/Pose endpoints.

- [ ] **Step 1: Create the handler**

Create `apps/admin/src/main/kotlin/com/neki/admin/config/ExceptionHandler.kt`:

```kotlin
package com.neki.admin.config

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.exception.dto.ExceptionMsg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * fileName       : ExceptionHandler
 * author         : koo
 * date           : 2026. 8. 16.
 * description    : admin 예외 전역처리. apps/api의 ExceptionHandler와 동일한 응답 형태를 쓴다.
 */
@RestControllerAdvice
class ExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun businessExceptionHandler(ex: BusinessException): ResponseEntity<ExceptionMsg> {
        log.warn("[BUSINESS_ERROR] code={} | message={}", ex.resultCode.code, ex.resultCode.message)

        return ResponseEntity(
            ExceptionMsg(resultCode = ex.resultCode.code, message = ex.resultCode.message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun methodArgumentNotValidExceptionHandler(ex: MethodArgumentNotValidException): ResponseEntity<ExceptionMsg> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: ResultCode.INVALID_PARAMETER.message

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.INVALID_PARAMETER.code, message = message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(Exception::class)
    fun exceptionHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.error("[SYSTEM_ERROR] unhandled exception", ex)

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.ERROR.code, message = ResultCode.ERROR.message),
            HttpStatus.BAD_REQUEST,
        )
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :apps:admin:compileKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add apps/admin/src/main/kotlin/com/neki/admin/config/ExceptionHandler.kt
git commit -m "feat: admin에 전역 예외 핸들러 추가"
```

---

## Task 8: Admin `SecurityConfig` + domain service wiring

**Files:**
- Create: `apps/admin/src/main/kotlin/com/neki/admin/auth/infra/NoopOidcTokenValidator.kt`
- Create: `apps/admin/src/main/kotlin/com/neki/admin/config/SecurityConfig.kt`
- Modify: `apps/admin/src/main/kotlin/com/neki/admin/config/DomainServiceConfig.kt`

- [ ] **Step 1: Create the no-op OIDC validator**

`AuthService`'s constructor requires an `OidcTokenValidator`, but admin never calls `validateOauthToken` (no social login). Create `apps/admin/src/main/kotlin/com/neki/admin/auth/infra/NoopOidcTokenValidator.kt`:

```kotlin
package com.neki.admin.auth.infra

import com.neki.domain.user.external.OidcTokenValidator
import com.neki.domain.user.models.OauthUserInfo
import com.neki.domain.user.models.Platform
import com.neki.domain.user.models.ProviderType

/**
 * fileName       : NoopOidcTokenValidator
 * author         : koo
 * date           : 2026. 8. 16.
 * description    : admin은 소셜 로그인을 지원하지 않는다. AuthService 생성자를 채우기 위한 미사용 구현체.
 */
class NoopOidcTokenValidator : OidcTokenValidator {
    override fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthUserInfo =
        throw UnsupportedOperationException("apps:admin 은 소셜 로그인을 지원하지 않는다.")
}
```

- [ ] **Step 2: Create `SecurityConfig`**

Create `apps/admin/src/main/kotlin/com/neki/admin/config/SecurityConfig.kt`:

```kotlin
package com.neki.admin.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.properties.AppProperties
import com.neki.domain.user.infra.security.filter.JwtAuthenticationFilter
import com.neki.domain.user.infra.security.handler.CustomAccessDeniedHandler
import com.neki.domain.user.infra.security.handler.CustomAuthenticationEntryPoint
import com.neki.domain.user.infra.security.token.AuthTokenProviderAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * fileName       : SecurityConfig
 * author         : koo
 * date           : 2026. 8. 16.
 * description    : /admin/v1/auth/** 를 제외한 모든 요청에 JWT 인증을 요구한다.
 *
 * domain.user.infra.security.* 의 클래스들을 재사용하되, 컴포넌트 스캔 대신 명시적 @Bean으로
 * 등록한다 (DomainServiceConfig와 동일한 패턴). domain.user.infra.security.config 패키지를
 * 스캔하면 apps/api 전용 SecurityConfig(/api/** 매처)와 OAuth 빈까지 함께 올라와 기동이
 * 실패하기 때문이다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val appProperties: AppProperties, private val objectMapper: ObjectMapper) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authTokenProviderAdapter(): AuthTokenProviderAdapter = AuthTokenProviderAdapter(appProperties)

    @Bean
    fun authenticationEntryPoint(): CustomAuthenticationEntryPoint = CustomAuthenticationEntryPoint(objectMapper)

    @Bean
    fun accessDeniedHandler(): CustomAccessDeniedHandler = CustomAccessDeniedHandler(objectMapper)

    @Bean
    fun jwtAuthenticationFilter(
        authTokenProviderAdapter: AuthTokenProviderAdapter,
        authenticationEntryPoint: CustomAuthenticationEntryPoint,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(authTokenProviderAdapter, authenticationEntryPoint)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        authenticationEntryPoint: CustomAuthenticationEntryPoint,
        accessDeniedHandler: CustomAccessDeniedHandler,
    ): SecurityFilterChain = http
        .securityMatcher("/**")
        .csrf { it.disable() }
        .authorizeHttpRequests {
            it.requestMatchers("/admin/v1/auth/**").permitAll()
            it.anyRequest().authenticated()
        }
        .exceptionHandling {
            it.authenticationEntryPoint(authenticationEntryPoint)
            it.accessDeniedHandler(accessDeniedHandler)
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}
```

- [ ] **Step 3: Wire `UserRepository` and `AuthService` in `DomainServiceConfig`**

`apps/admin/src/main/kotlin/com/neki/admin/config/DomainServiceConfig.kt` currently reads:

```kotlin
package com.neki.admin.config

import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.map.service.BrandService
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : DomainServiceConfig
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : admin 이 쓰는 도메인 서비스만 명시적으로 등록한다
 *
 * :domain 을 패키지 단위로 스캔하면 admin 이 쓰지 않는 도메인 서비스까지 빈으로 올라오고,
 * 그 서비스들의 포트 어댑터를 전부 요구해 기동이 실패한다.
 */
@Configuration
class DomainServiceConfig {

    @Bean
    fun brandService(brandRepository: BrandRepository): BrandService = BrandService(brandRepository)

    @Bean
    fun poseService(poseRepository: PoseRepository): PoseService = PoseService(poseRepository)
}
```

Change it to:

```kotlin
package com.neki.admin.config

import com.neki.admin.auth.infra.NoopOidcTokenValidator
import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.map.service.BrandService
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseService
import com.neki.domain.user.infra.persist.UserRepositoryAdapter
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.infra.security.token.AuthTokenProviderAdapter
import com.neki.domain.user.repository.UserRepository
import com.neki.domain.user.service.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * fileName       : DomainServiceConfig
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : admin 이 쓰는 도메인 서비스만 명시적으로 등록한다
 *
 * :domain 을 패키지 단위로 스캔하면 admin 이 쓰지 않는 도메인 서비스까지 빈으로 올라오고,
 * 그 서비스들의 포트 어댑터를 전부 요구해 기동이 실패한다.
 */
@Configuration
class DomainServiceConfig {

    @Bean
    fun brandService(brandRepository: BrandRepository): BrandService = BrandService(brandRepository)

    @Bean
    fun poseService(poseRepository: PoseRepository): PoseService = PoseService(poseRepository)

    @Bean
    fun userRepository(jpaUserRepository: JpaUserRepository): UserRepository =
        UserRepositoryAdapter(jpaUserRepository)

    @Bean
    fun authService(
        authTokenProviderAdapter: AuthTokenProviderAdapter,
        passwordEncoder: PasswordEncoder,
        userRepository: UserRepository,
    ): AuthService = AuthService(
        tokenProviderPort = authTokenProviderAdapter,
        oidcTokenValidatorPort = NoopOidcTokenValidator(),
        passwordEncoder = passwordEncoder,
        userRepository = userRepository,
    )
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :apps:admin:compileKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 5: Commit**

```bash
git add apps/admin/src/main/kotlin/com/neki/admin/auth/infra/NoopOidcTokenValidator.kt \
  apps/admin/src/main/kotlin/com/neki/admin/config/SecurityConfig.kt \
  apps/admin/src/main/kotlin/com/neki/admin/config/DomainServiceConfig.kt
git commit -m "feat: admin에 JWT 보안 필터체인과 AuthService 배선 추가"
```

---

## Task 9: Login/refresh endpoints

**Files:**
- Create: `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminDto.kt`
- Create: `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminMapper.kt`
- Create: `apps/admin/src/main/kotlin/com/neki/admin/auth/application/AuthAdminFacade.kt`
- Create: `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminController.kt`

- [ ] **Step 1: Create the DTOs**

Create `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminDto.kt`:

```kotlin
package com.neki.admin.auth.api

import jakarta.validation.constraints.NotBlank

/**
 * fileName       : AuthAdminDto
 * author         : koo
 * date           : 2026. 8. 16.
 * description    :
 */
object AuthAdminDto {
    class Request {
        data class Login(
            @field:NotBlank(message = "email은 필수 입력값입니다.")
            val email: String?,
            @field:NotBlank(message = "password는 필수 입력값입니다.")
            val password: String?,
        )

        data class Refresh(
            @field:NotBlank(message = "refreshToken은 필수 입력값입니다.")
            val refreshToken: String?,
        )
    }

    class Response {
        data class Token(val accessToken: String, val refreshToken: String)
    }
}
```

- [ ] **Step 2: Create the mapper**

Create `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminMapper.kt`:

```kotlin
package com.neki.admin.auth.api

import com.neki.domain.user.dto.AuthCommand

/**
 * fileName       : AuthAdminMapper
 * author         : koo
 * date           : 2026. 8. 16.
 * description    :
 */
fun AuthAdminDto.Request.Refresh.toCommand(): AuthCommand.RefreshToken = AuthCommand.RefreshToken(refreshToken!!)
```

- [ ] **Step 3: Create the facade**

Create `apps/admin/src/main/kotlin/com/neki/admin/auth/application/AuthAdminFacade.kt`:

```kotlin
package com.neki.admin.auth.application

import com.neki.admin.auth.api.AuthAdminDto
import com.neki.domain.user.dto.AuthCommand
import com.neki.domain.user.models.IssuedTokens
import com.neki.domain.user.models.User
import com.neki.domain.user.service.AuthService
import org.springframework.stereotype.Service

/**
 * fileName       : AuthAdminFacade
 * author         : koo
 * date           : 2026. 8. 16.
 * description    :
 */
@Service
class AuthAdminFacade(private val authService: AuthService) {

    fun login(email: String, password: String): AuthAdminDto.Response.Token {
        val user: User = authService.authenticateByEmail(email, password)
        val tokens: IssuedTokens = authService.issueTokens(user)
        return AuthAdminDto.Response.Token(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    fun refresh(command: AuthCommand.RefreshToken): AuthAdminDto.Response.Token {
        val tokens: IssuedTokens = authService.rotateTokens(command)
        return AuthAdminDto.Response.Token(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }
}
```

- [ ] **Step 4: Create the controller**

Create `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminController.kt`:

```kotlin
package com.neki.admin.auth.api

import com.neki.admin.auth.application.AuthAdminFacade
import com.neki.core.api.dto.BaseResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : AuthAdminController
 * author         : koo
 * date           : 2026. 8. 16.
 * description    :
 */
@RestController
@RequestMapping("/admin/v1/auth")
class AuthAdminController(private val authAdminFacade: AuthAdminFacade) {

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: AuthAdminDto.Request.Login): BaseResponse<AuthAdminDto.Response.Token> =
        BaseResponse(data = authAdminFacade.login(request.email!!, request.password!!))

    @PostMapping("/refresh")
    fun refresh(@RequestBody @Valid request: AuthAdminDto.Request.Refresh): BaseResponse<AuthAdminDto.Response.Token> =
        BaseResponse(data = authAdminFacade.refresh(request.toCommand()))
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew :apps:admin:compileKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 6: Commit**

```bash
git add apps/admin/src/main/kotlin/com/neki/admin/auth/
git commit -m "feat: admin 로그인/토큰 갱신 API 추가"
```

---

## Task 10: Seed data (Postgres migration + local H2)

**Files:**
- Create: `modules/postgres/src/main/resources/db/migration/V28__seed_admin_user.sql`
- Create: `apps/admin/src/main/resources/data-local.sql`

Both seeds use the same placeholder BCrypt hash for the password `admin1234` — real, valid bcrypt output, safe for local dev. The migration comment makes clear it must be replaced before any real deployment; nobody should ship this password to production.

- [ ] **Step 1: Create the Postgres migration**

Create `modules/postgres/src/main/resources/db/migration/V28__seed_admin_user.sql`:

```sql
-- 관리자 계정 시드 (apps/admin 로그인용).
-- password는 "admin1234"의 BCrypt 해시(placeholder)다. 배포 전 반드시 실제 비밀번호로 교체할 것 —
-- BCryptPasswordEncoder로 새로 해싱한 값을 UPDATE TB_USERS SET password = '...' WHERE email = 'admin@neki.com' 으로 반영한다.
INSERT INTO TB_USERS (email, password, oid, name, provider_type, profile_image_id, role, created_at, updated_at)
VALUES (
    'admin@neki.com',
    '$2b$10$WBYvWYfDobbLF1UnJ.bp/eGormNUWmmJ23S6zdnqiUZdqWfzPm0Ky',
    NULL,
    'admin',
    'LOCAL',
    NULL,
    'ROLE_USER,ROLE_ADMIN',
    now(),
    now()
);
```

- [ ] **Step 2: Create the local dev seed**

Create `apps/admin/src/main/resources/data-local.sql`:

```sql
-- 로컬 개발용 관리자 계정. email=admin@neki.com, password=admin1234
INSERT INTO TB_USERS (email, password, oid, name, provider_type, profile_image_id, role, created_at, updated_at)
VALUES (
    'admin@neki.com',
    '$2b$10$WBYvWYfDobbLF1UnJ.bp/eGormNUWmmJ23S6zdnqiUZdqWfzPm0Ky',
    NULL,
    'admin',
    'LOCAL',
    NULL,
    'ROLE_USER,ROLE_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

- [ ] **Step 3: Boot admin locally and verify the seed + login work end to end**

Run: `./gradlew :apps:admin:bootRun --args='--spring.profiles.active=local'` (leave running)

In another terminal:

```bash
curl -s -X POST http://localhost:8081/admin/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@neki.com","password":"admin1234"}'
```

Expected: `{"resultCode":"D-0","message":"OK","data":{"accessToken":"...","refreshToken":"..."}}`

```bash
curl -s -X POST http://localhost:8081/admin/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@neki.com","password":"wrong"}'
```

Expected: `{"resultCode":"D-13","message":"이메일 또는 비밀번호가 올바르지 않습니다.","data":null}`

```bash
curl -s http://localhost:8081/admin/v1/pose
```

Expected: `{"resultCode":"D-996","message":"토큰이 존재하지 않습니다.","data":null}` — the existing Pose admin endpoint is now guarded.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/admin/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@neki.com","password":"admin1234"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

curl -s http://localhost:8081/admin/v1/pose -H "Authorization: Bearer $TOKEN"
```

Expected: `{"resultCode":"D-0","message":"OK","data":{"headCount":null,"totalCount":0,"totalPages":0,"poses":[]}}` (empty list is fine — no poses seeded).

Stop the running `bootRun` process (Ctrl+C) once verified.

- [ ] **Step 4: Commit**

```bash
git add modules/postgres/src/main/resources/db/migration/V28__seed_admin_user.sql apps/admin/src/main/resources/data-local.sql
git commit -m "feat: 관리자 계정 시드 추가 (postgres 마이그레이션 + 로컬 H2)"
```

---

## Task 11: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Format**

Run: `./gradlew spotlessApply -q`
Expected: no output, exit code 0.

If `git status --short` shows changes, commit them:

```bash
git add -A
git commit -m "chore: spotlessApply"
```

- [ ] **Step 2: Full build**

Run: `./gradlew build -x bootJar`
Expected: `BUILD SUCCESSFUL`, all modules compile, all tests (including the new `AuthServiceTest` and the two updated apps/api tests) pass.

- [ ] **Step 3: Re-run the manual login/guard smoke test from Task 10 Step 3**

Confirms nothing in Task 11 broke the end-to-end flow. Same three curl calls, same expected responses.

---

## Plan self-review notes

- **Spec coverage**: §3 (account model) → Task 2, 3. §4 (endpoints) → Task 9. §5 (guard existing endpoints) → Task 8. §6 (wiring) → Task 4, 5, 6. §7 (seed) → Task 10. §9 (tests) → Task 3. All covered.
- **Known gap carried from spec §8-1**: `apps/admin` still has no staging/prod Postgres profile. Task 10 Step 3 only proves the flow against local H2 — the Postgres migration in Step 1 will sit unapplied until that profile exists. This matches the spec's explicit out-of-scope note.
- **Type consistency checked**: `AuthService` constructor order (`tokenProviderPort, oidcTokenValidatorPort, passwordEncoder, userRepository`) is identical across Task 3's production code, Task 3's test, Task 3's two apps/api test fixes, and Task 8's `DomainServiceConfig` bean. `IssuedTokens`, `AuthCommand.RefreshToken`, `AuthAdminDto.Response.Token` field names match between `AuthAdminFacade` and `AuthAdminController` usage.
