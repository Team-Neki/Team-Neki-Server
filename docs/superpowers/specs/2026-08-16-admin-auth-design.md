# admin 인증 API 설계 (Admin Auth)

- **작성일**: 2026-08-16
- **브랜치**: `feat/BACKEND-83-admin-app`
- **목표**: `apps/admin`에 아이디/비밀번호 기반 로그인 API를 추가하고, JWT로 기존 Brand/Pose admin 엔드포인트를 보호한다.
- **소셜 로그인 없음**: 카카오/애플 OIDC는 쓰지 않는다. 이메일 + 비밀번호만 지원한다.

---

## 1. 배경 & 동기

`apps/admin`은 Brand/Pose CRUD가 이미 붙어 있지만 인증이 전혀 없다. 아무나 `/admin/v1/**`를 호출할 수 있다.

확인된 사실:

- `User` 엔티티(`domain.user.models.User`)에 이미 `password: String`, `email: String?`, `roles: String` 필드가 있다. 소셜 로그인 사용자는 `password = "NO_PASS"`로 채워진다.
- `RoleType.ADMIN`("ROLE_ADMIN")이 이미 존재한다. 현재는 `apps/api`의 `/api/poses/admin/upload` 하나만 `hasRole("ADMIN")`으로 막혀 있다 — 즉 "관리자 권한을 가진 User"라는 개념 자체는 이미 있다.
- `UserPrincipal`에 `constructor(user: User)`가 이미 있고 주석이 "LOCAL 로그인 생길 시"다 — 이 기능이 이미 예정돼 있었다는 뜻이다.
- JWT 발급/검증 스택(`AuthTokenProvider`/`AuthTokenProviderAdapter`, `JwtAuthenticationFilter`, `CustomAuthenticationEntryPoint`, `CustomAccessDeniedHandler`, `AuthService.issueTokens/rotateTokens`)이 전부 `domain.user.infra.security.*`에 이미 있고, OAuth에 특화된 부분이 없다 — 순수 JWT 발급/파싱/에러응답 로직이다.
- `apps/admin`은 지금 `spring-boot-starter-security`, jjwt 의존성이 없고, `AppProperties`(`@ConfigurationProperties`) 바인딩도 안 돼 있다.
- `apps/admin`은 로컬 프로필에서 H2 인메모리(`ddl-auto: create-drop`, `flyway.enabled: false`)만 쓴다. staging/prod처럼 `apps/api`와 같은 PostgreSQL을 보는 프로필이 아직 없다.

결론: **User + RoleType.ADMIN을 그대로 재사용**하고, 이미 만들어진 JWT 스택을 admin에 명시적으로 배선하는 것이 가장 적은 코드로 가는 길이다.

---

## 2. 접근 방식 비교

**A. 기존 `domain.user.infra.security.*` 재사용 (채택)**
`DomainServiceConfig`가 지금 하고 있는 패턴 — 컴포넌트 스캔 대신 명시적 `@Bean` — 을 그대로 따라간다. 이미 짜여 있고 간접적으로 검증된 코드를 재사용한다.

**B. admin 전용 JWT 스택을 처음부터 새로 작성**
격리는 확실하지만 이미 올바르게 동작하는 코드 150줄 이상을 복제한다. 채택하지 않음.

---

## 3. 계정/비밀번호 모델

- 새 엔티티 없음. `User`(`domain.user.models`) 그대로 사용.
- `UserRepository`(`domain.user.repository`)에 `findByEmail(email: String): User?` 추가.
  - `JpaUserRepository`에 Spring Data 파생 쿼리 `findByEmail` 추가.
  - `UserRepositoryAdapter`에 위임 메서드 추가.
- `AuthService`(`domain.user.service`)에 추가:
  ```kotlin
  fun authenticateByEmail(email: String, rawPassword: String): User
  ```
  - `userRepository.findByEmail(email)` → 없으면 실패.
  - `passwordEncoder.matches(rawPassword, user.password)` → 틀리면 실패.
  - `user.roles`에 `RoleType.ADMIN.role`이 없으면 실패.
  - 세 경우 모두 **동일한** `BusinessException(ResultCode.INVALID_CREDENTIALS)`을 던진다 (이메일 존재 여부가 응답으로 드러나지 않도록).
  - `AuthService`는 새 생성자 파라미터로 `PasswordEncoder`, `UserRepository`를 받는다.
- `ResultCode`에 `INVALID_CREDENTIALS` 신규 추가 (401).
- `PasswordEncoder`는 `BCryptPasswordEncoder` 빈으로 등록 (`apps/admin`에서 `@Bean`).

---

## 4. 엔드포인트

Brand/Pose와 동일한 admin 레이어 패턴(Controller → Mapper → Facade → 도메인 Service)을 따른다.

- `POST /admin/v1/auth/login`
  - Request: `{ email: String, password: String }`
  - `AdminAuthFacade.login()` → `authService.authenticateByEmail(...)` → `authService.issueTokens(user)`
  - Response: `{ accessToken: String, refreshToken: String }`
- `POST /admin/v1/auth/refresh`
  - Request: `{ refreshToken: String }`
  - `authService.rotateTokens(AuthCommand.RefreshToken(refreshToken))` 그대로 재사용 (수정 없음)
  - Response: `{ accessToken: String, refreshToken: String }`

새 파일: `apps/admin/src/main/kotlin/com/neki/admin/auth/api/AuthAdminController.kt`,
`AuthAdminDto.kt`, `AuthAdminMapper.kt`, `apps/admin/.../auth/application/AuthAdminFacade.kt`.

---

## 5. 기존 Brand/Pose 엔드포인트 보호

새 파일 `apps/admin/src/main/kotlin/com/neki/admin/config/SecurityConfig.kt`:

- `@EnableWebSecurity`, 단일 `SecurityFilterChain`, `securityMatcher("/**")`.
- `/admin/v1/auth/**`만 `permitAll()`, 나머지 `anyRequest().authenticated()`.
- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 연결.
- `exceptionHandling`에 `CustomAuthenticationEntryPoint`/`CustomAccessDeniedHandler` 연결.
- 로그인 시점에 이미 `ROLE_ADMIN`을 검증하므로 별도 `hasRole` 매처는 두지 않는다 — 토큰이 발급됐다는 것 자체가 관리자라는 뜻이다.
- CORS는 이번 범위에서 뺀다 (admin 프론트 없음, 필요해지면 추가).

이 클래스들(`AuthTokenProviderAdapter`, `JwtAuthenticationFilter`, `CustomAuthenticationEntryPoint`,
`CustomAccessDeniedHandler`)은 컴포넌트 스캔이 아니라 `SecurityConfig` 안에서 **명시적 `@Bean`**으로
생성자를 직접 호출해 등록한다 (`DomainServiceConfig`와 동일한 방식). `AdminApplication`의
`scanBasePackages`는 건드리지 않는다 — `domain.user.infra.security.config.SecurityConfig`(apps/api
전용, `/api/**` 매처)나 OAuth 관련 빈이 함께 스캔되는 걸 피하기 위해서다.

---

## 6. 배선 (build.gradle.kts / Application / yaml)

- `apps/admin/build.gradle.kts`: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api/impl/jackson`(0.12.5, 기존과 동일 버전) 추가.
- `AdminApplication`: `@ConfigurationPropertiesScan("com.neki.core")` 추가 (`AppProperties` 바인딩 활성화). `domain`은 여전히 스캔하지 않는다.
- `apps/admin/src/main/resources/application.yaml`, `apps/admin/src/test/resources/application-test.yml`:
  `apps/api`의 테스트 프로필과 동일하게 Jasypt 없이 평문 시크릿으로 `app.auth.*` 추가
  (`accessTokenSecret`, `accessTokenExpiry`, `refreshTokenSecret`, `refreshTokenExpiry`).
  admin은 아직 Jasypt 모듈을 안 쓰므로 운영 환경 암호화는 이번 범위 밖.

---

## 7. 계정 생성 (시드)

API로 계정을 만들지 않는다 — 로그인 API만 제공한다.

- `modules/postgres/src/main/resources/db/migration/`에 신규 Flyway 마이그레이션 1개:
  관리자 `User` 1행 삽입 (`provider_type='LOCAL'`, `oid=NULL`, `role='ROLE_USER,ROLE_ADMIN'`,
  `password`는 placeholder BCrypt 해시 + "배포 전 실제 값으로 교체" 주석). 실제 운영 비밀번호는
  임의로 만들지 않는다.
  - 이 마이그레이션은 `apps/api`가 쓰는 것과 같은 공유 PostgreSQL에 적용된다 — `apps/admin`
    자체의 (현재 H2뿐인) 데이터소스와는 무관하다.
- 로컬 개발 편의용으로 `apps/admin/src/main/resources/data-local.sql`(Spring Boot `spring.sql.init`)에
  같은 시드를 하나 더 둬서, `./gradlew :apps:admin:bootRun`을 로컬에서 바로 켜도 로그인 테스트가
  가능하게 한다. 원문 비밀번호는 로컬 전용이라는 걸 문서화한다 (예: `admin1234`).

---

## 8. 트레이드오프 & 남겨두는 것

1. **admin의 실제 DB 연결**: `apps/admin`은 아직 staging/prod에서 `apps/api`와 같은 PostgreSQL을
   보는 프로필이 없다 (H2 로컬뿐). 이번 작업 범위 밖 — 이 기능은 그 배선이 끝나야 실제 배포 환경에서
   동작한다. 로컬 개발/테스트에서는 완결된 흐름으로 검증한다.
2. **CORS 미포함**: admin 전용 프론트엔드가 아직 없어서 뺐다. 프론트가 붙으면
   `CorsConfigurationSource` 빈을 추가해야 한다.
3. **역할 매처 없음**: 로그인 시점 검증으로 충분하다고 판단해 `hasRole("ADMIN")` 같은 경로별
   매처를 두지 않았다. admin 안에 권한 등급이 갈리면(예: 슈퍼관리자 vs 운영자) 그때 추가한다.
4. **회원가입/계정 관리 API 없음**: 의도적. 계정은 마이그레이션으로만 생긴다.

---

## 9. 테스트

- `AuthService.authenticateByEmail` 단위 테스트(정상, 비밀번호 틀림, 이메일 없음, ROLE_ADMIN 없음) —
  MockK로 `UserRepository`/`PasswordEncoder` 모킹.
- Controller/E2E 테스트는 두지 않는다 — `apps/admin` 전체에 아직 E2E 인프라(테스트용 DB 컨테이너,
  `AdminE2ETestBase` 등)가 없는 기존 공백이고, 이번 기능만을 위해 새로 만들지 않는다.

---

## 10. 검증 기준 (Definition of Done)

- `./gradlew build` 전체 성공 (컴파일 + 전체 테스트).
- `./gradlew :apps:admin:bootRun` 로 로컬 기동 성공, `data-local.sql` 시드 계정으로 로그인 성공.
- 로그인으로 받은 토큰으로 기존 Brand/Pose admin 엔드포인트 호출 성공, 토큰 없이 호출 시 401.
- `spotlessApply` 통과.

---

## 11. 미해결/구현 단계에서 결정할 사항

- `apps/admin`의 staging/prod PostgreSQL 프로필 배선 (§8-1) — 후속 작업.
- refresh token 회전(rotation) 시 기존 토큰 폐기 여부 — 지금 `AuthService.rotateTokens`는 blacklist가
  없다. `apps/api`도 같은 상태라 이번에 새로 만들지 않는다.
