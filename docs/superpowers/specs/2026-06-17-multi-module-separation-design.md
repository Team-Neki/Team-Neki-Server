# 멀티 모듈 분리 설계 (Multi-Module Separation)

- **작성일**: 2026-06-17
- **브랜치**: `poc/multi-module`
- **목표**: 단일 모듈 Spring Boot 프로젝트를 레이어/의존성 기반 멀티 Gradle 모듈로 재편한다.
- **이번 POC 범위**: **전체 마이그레이션** (7개 도메인 + 모든 인프라를 한 번에 모듈로 이동)

---

## 1. 배경 & 동기

현재 `Team-Neki-Server`는 단일 Gradle 모듈이다. 모든 코드가 `com.neki` 패키지 아래에 도메인별
(api/application/domain/infra) 구조로 존재한다. 공통 코드는 `com.neki.common`에 모여 있다.

확인된 사실:

- `common` 패키지는 **어떤 도메인에도 역의존하지 않는다** → 순환 의존 없이 추출 가능.
- 도메인 → common 참조: photo(31), user(19), pose(18), support(11), media(10), map(8).
- 진입점: `com.neki.NekiApplication` (`@SpringBootApplication` + `@ConfigurationPropertiesScan`,
  base package = `com.neki`).
- `common`은 얇은 커널이 아니다 — web, jpa, redis, querydsl, swagger, jasypt를 직접 끌어다 쓴다.

분리 목적:

- 외부 시스템 의존성(DB/캐시/스토리지/외부 API)을 기술별 독립 모듈로 격리.
- 레이어(domain / application) 경계를 모듈 경계로 강제.
- 설정값을 의존성 모듈별로 분리하고 bootstrap에서 통합.

---

## 2. 모듈 토폴로지

```
root
├── bootstrap          # 실행/조립 전용 (composition root)
├── core               # 순수 공유 커널 (외부 시스템 연결 없음)
├── domain             # 엔티티/VO/enum
├── application        # api + usecase/port + 웹 보안 기계장치 + 공통 웹 설정
└── modules/
    ├── postgres       # JPA/QueryDSL/Flyway 영속성 어댑터 (전 도메인)
    ├── redis          # Redis 캐시 어댑터
    ├── s3             # AWS S3 스토리지 어댑터
    ├── kakao          # Kakao Map API + Kakao OIDC 클라이언트
    ├── apple          # Apple OIDC 클라이언트
    └── discord        # Discord 웹훅 (notification)
```

### 의존 방향 (안쪽일수록 의존 없음)

```
bootstrap ──depends on──▶ 모든 모듈
modules/* ──▶ application, domain, core
application ──▶ domain, core
domain ──▶ core
core ──▶ (외부 시스템 연결 없음; Spring/JPA/swagger 어노테이션은 허용)
```

**핵심 원칙**

1. `modules/*` = 외부 시스템(외부 DB / 외부 API / 캐시 / 스토리지) 의존성을 격리하는 곳.
2. `core` = 외부 시스템에 연결되는 의존성을 포함하지 않는다. (단, Spring/JPA/swagger **어노테이션**
   수준 의존은 허용 — 인프라 연결만 금지)

---

## 3. 파일 배치 (현재 → 목표)

### core
순수 공유 커널. Spring/JPA/swagger 어노테이션은 허용하되 인프라 연결은 금지.

- `common/api/dto/ResultCode.kt`
- `common/exception/BusinessException.kt`
- `common/exception/dto/ExceptionDto.kt`
- `common/domain/vo/SortOrder.kt`
- `common/domain/BaseTimeEntity.kt` (JPA `@MappedSuperclass`)
- `common/annotation/UseCase.kt` (spring-context `@Component`)
- `common/transaction/TransactionRunner.kt` (spring-tx)
- `common/api/dto/BaseResponse.kt` (swagger `@Schema`)

### domain
순수 엔티티/값 객체. (엔티티의 JPA 어노테이션 의존은 허용)

- 7개 도메인의 `*/domain/entity/**`
- 7개 도메인의 `*/domain/enums/**`
- 도메인 VO

### application
api 레이어 + 유스케이스 레이어 + 웹 보안 기계장치 + 공통 웹 설정.

- 전 도메인 `*/api/**` (controller, converter, dto)
- 전 도메인 `*/application/**` (usecase, port, command, result)
- Spring Security 기계장치:
  `user/infra/security/{config/SecurityConfig, filter/*, handler/*, token/AuthTokenProvider, token/UserPrincipal}`
- OAuth **추상화 인터페이스 + 레지스트리**:
  `oauth/oidc/Oidc`, `oauth/helper/OauthHelper`, `oauth/registry/OidcRegistry`,
  `oauth/registry/OauthHelperRegistry`, `oauth/OidcTokenValidator`, `security/config/OauthProperties`
- 공통 웹 설정:
  `common/api/config/ObjectMapperConfig`, `common/api/config/WebConfig`(CORS),
  `common/api/document/SwaggerConfig`, `common/api/document/RequiresSecurity`,
  `common/exception/handler/ExceptionHandler`,
  `common/filter/{RequestMdcFilter, RequestLoggingFilter, ServletFilterConfig}`
- `common/properties/AppProperties`
- 내부 크로스 도메인 클라이언트: `user/infra/client/{UserMediaClient, UserTermClient}`,
  `user/infra/event/UserEventPublisher`, `user/infra/generator/NicknameGenerator`,
  notification 이벤트 리스너 `notification/infra/discord/UserDiscordListener`

### modules/postgres
- 전 도메인 JPA 영속성 어댑터: `*/infra/persist/**` (예: `FolderRepositoryAdapter`,
  `UserRepositoryAdapter`, …)
- QueryDSL 리포지토리: `*QueryRepository`
- `common/infra/config/QueryDslConfig`, `common/infra/config/JpaAuditingConfig`
- Flyway 마이그레이션: `src/main/resources/db/migration/**` (스키마 소유)
- PostGIS / Hibernate Spatial / JTS
- `application-postgres.yaml` (datasource, jpa, flyway 설정)

### modules/redis
- `common/config/RedisCacheConfig`
- 캐시 어댑터: `user/infra/cache/redis/AuthRedisCacheAdapter`
  (그리고 fake 변형 `user/infra/cache/fake/AuthInMemoryCacheAdapter`)
- `application-redis.yaml`

### modules/s3
- media S3 스토리지 어댑터: `media/infra/storage/s3/**` (+ `media/infra/storage/fake/**`)
- AWS SDK v2
- `application-s3.yaml`

### modules/kakao
- Kakao Map API 클라이언트: `map/infra/client/kakao/**` (+ fake `map/infra/client/fake/**`)
- Kakao OIDC 구현체: `user/infra/security/oauth/{helper/KakaoOauthHelper, oidc/KakaoOidc}`
- `application-kakao.yaml`

### modules/apple
- Apple OIDC 구현체: `user/infra/security/oauth/{helper/AppleOauthHelper, oidc/AppleOidc}`
- `application-apple.yaml`

### modules/discord
- Discord 웹훅 발신 어댑터 + `notification/properties/DiscordProperties`
- `application-discord.yaml`
- (이벤트 리스너 `UserDiscordListener`는 application에, 외부 발신만 이 모듈)

### bootstrap
실행/조립 전용. 코드 거의 없이 조립과 실행만 담당.

- `NekiApplication.kt` (`@SpringBootApplication`, 컴포넌트 스캔 루트 `com.neki`)
- `common/config/JasyptConfig` (기동 시 properties 복호화)
- `common/config/AsyncConfig` (`@EnableAsync` — 앱 전역 토글)
- `common/infra/config/RestClientConfig` (공유 RestClient 빈)
- `src/main/resources/`: `application*.yaml`, `logback-spring.xml`
- `spring-boot` 플러그인 + `bootJar` (Layered JAR)
- **모든 모듈에 대한 의존성 선언** (composition root)

---

## 4. 설정값(Config) 전략

- 각 `modules/*`는 자신의 `application-<name>.yaml`(기본값/구조)을 소유한다.
- bootstrap이 `spring.config.import`로 모듈별 yaml을 통합한다.
- 패키지명을 `com.neki.*`로 **유지**하므로 컴포넌트 스캔(base=`com.neki`)과
  `@ConfigurationPropertiesScan`이 classpath 전체를 스캔 → **도메인 코드 import 변경 0건**.

---

## 5. Gradle 빌드 구조

- 루트 `build.gradle.kts`: 공통 플러그인(kotlin, spring dependency-management, spotless, kapt) 및
  버전 카탈로그를 `subprojects`/`allprojects` 또는 convention plugin으로 공유.
- 각 모듈 `build.gradle.kts`: 자신이 필요한 의존성만 선언.
- `spring-boot` 플러그인의 `bootJar`는 **bootstrap에만** 적용. 나머지 모듈은 라이브러리 jar
  (`bootJar { enabled = false }`, `jar { enabled = true }`).
- `settings.gradle.kts`: `include(":core", ":domain", ":application", ":bootstrap",
  ":modules:postgres", ":modules:redis", ":modules:s3", ":modules:kakao", ":modules:apple",
  ":modules:discord")`.
- **QueryDSL kapt**: Q클래스는 엔티티가 있는 모듈에서 생성되어야 한다. 엔티티는 `domain` 모듈에 있고
  QueryDSL 리포지토리는 `modules/postgres`에 있으므로, kapt 적용 위치와 Q클래스 가시성을 명확히
  해야 한다 (Q클래스는 엔티티 기준 생성 → `domain` 또는 `modules/postgres` 중 kapt 적용 모듈 결정 필요;
  구현 단계에서 검증).

---

## 6. 주요 트레이드오프 & 리스크

1. **application의 굵은 의존성**: api + usecase + ports가 한 모듈에 있어 `modules/*`가 application에
   의존할 때 web(controller)까지 전이 의존된다. 추후 `application-api` / `application-core` 분리
   여지. POC에서는 통합 유지.
2. **OAuth 추상화 분리**: `Oidc`/`OauthHelper` 인터페이스와 레지스트리는 application에, 구현만
   modules(kakao/apple)에 둔다. 안 그러면 모듈 간/공통 추상화 결합으로 독립성이 깨진다.
3. **QueryDSL kapt 위치**: 모듈 분리 시 Q클래스 생성/가시성 문제가 가장 큰 기술 리스크. 구현 초기에
   별도 검증 슬라이스로 확인.
4. **테스트 경계**: `E2ETestBase`, ArchUnit 규칙, `JasyptTest`가 모듈 경계에 맞게 재배치/수정 필요.
   ArchUnit 의존성 규칙은 모듈 의존 그래프를 강제하도록 재작성 후보.
5. **Flyway 마이그레이션 위치**: `modules/postgres`가 스키마를 소유 (resources/db/migration 이동).

---

## 7. 검증 기준 (Definition of Done)

- `./gradlew build` 전체 성공 (모든 모듈 컴파일 + 테스트).
- `./gradlew :bootstrap:bootRun` (또는 동등) 으로 앱 기동 성공.
- 기존 E2E 테스트 전부 통과.
- 모듈 의존 그래프가 §2 의존 방향을 위반하지 않음 (가능하면 ArchUnit/Gradle로 강제).
- `spotlessApply` 통과.

---

## 8. 미해결/구현 단계에서 결정할 사항

- QueryDSL kapt 적용 모듈 최종 결정 (domain vs modules/postgres).
- 루트 빌드 공통화 방식: `subprojects {}` vs convention plugin(`buildSrc`/`build-logic`).
- `RestClientConfig` 공유 빈 위치 최종 확정 (bootstrap vs 각 외부 모듈 자체 정의).
- ArchUnit 규칙의 멀티 모듈 대응 방식.
