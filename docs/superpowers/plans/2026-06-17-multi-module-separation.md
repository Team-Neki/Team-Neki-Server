# 멀티 모듈 분리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단일 모듈 Spring Boot 프로젝트를 레이어/의존성 기반 멀티 Gradle 모듈(core / domain / application / modules:* / bootstrap)로 재편하고, 전체 코드(287개 main 파일)를 마이그레이션한다.

**Architecture:** 루트는 얇은 aggregator로 두고 빌드 공통 설정만 제공한다. 먼저 전체 소스를 단일 `:bootstrap` 모듈로 통째 이동해 빌드 green을 확보한 뒤, 의존성 안쪽(core)부터 바깥(modules)으로 한 레이어씩 추출한다. 매 추출 단계에서 bootstrap이 새 모듈에 의존하므로 컴파일/테스트가 항상 통과한다. 마지막에 bootstrap을 실행/조립 전용으로 정리한다.

**Tech Stack:** Kotlin 2.0.10, Java 21, Spring Boot 3.5.8, Gradle 8.14.3 (Kotlin DSL), QueryDSL 5.0.0(kapt), JPA/PostgreSQL, Redis, AWS S3, Flyway, springdoc, Jasypt, Kotest/MockK/RestAssured/ArchUnit.

**설계 문서:** `docs/superpowers/specs/2026-06-17-multi-module-separation-design.md`

---

## 사전 합의된 핵심 결정

- **domain**: 단일 모듈 (엔티티/VO/enum). QueryDSL **kapt는 domain 모듈에서** 실행해 Q클래스를 생성한다(엔티티가 domain에 있으므로). `modules:postgres`는 domain에 의존해 Q클래스를 사용한다.
- **application**: api(controller/converter/dto) + usecase/port/command/result + Spring Security 기계장치 + 공통 웹 설정 + OAuth 추상화 인터페이스.
- **modules/***: postgres, redis, s3, kakao, apple, discord (외부 시스템별 어댑터 + 모듈별 yaml).
- **core**: 순수 공유 커널. Spring/JPA/swagger **어노테이션 의존만 허용**, 외부 시스템 연결 금지.
- **bootstrap**: NekiApplication, JasyptConfig, AsyncConfig, RestClientConfig, resources(yaml/logback), bootJar. 전 모듈 의존.
- **패키지명 유지**: 모든 클래스는 `com.neki.*` 패키지를 유지한다 → 컴포넌트 스캔/`@ConfigurationPropertiesScan`(base=`com.neki`)이 classpath 전체를 스캔하므로 **도메인 코드 import 변경 0건**.

## 파일 → 모듈 매핑 요약

| 모듈 | 소스 글롭(현재 위치 기준) |
|------|--------------------------|
| core | `common/api/dto/{ResultCode,BaseResponse}.kt`, `common/exception/BusinessException.kt`, `common/exception/dto/ExceptionDto.kt`, `common/domain/vo/SortOrder.kt`, `common/domain/BaseTimeEntity.kt`, `common/annotation/UseCase.kt`, `common/transaction/TransactionRunner.kt` |
| domain | `*/domain/entity/**`, `*/domain/enums/**`, 도메인 VO (7개 도메인) |
| application | `*/api/**`, `*/application/**`, `user/infra/security/{config/SecurityConfig,filter,handler,token}`, `user/infra/security/oauth/{Oidc,OauthHelper,OidcTokenValidator,registry/*,config/OauthProperties}`, `user/infra/{client,event,generator}`, `notification/infra/discord/UserDiscordListener`, `common/api/config/**`, `common/api/document/**`, `common/exception/handler/**`, `common/filter/**`, `common/properties/AppProperties.kt` |
| modules:postgres | `*/infra/persist/**`, `*QueryRepository`, `common/infra/config/{QueryDslConfig,JpaAuditingConfig}.kt`, `resources/db/migration/**` |
| modules:redis | `common/config/RedisCacheConfig.kt`, `user/infra/cache/{redis,fake}/**` |
| modules:s3 | `media/infra/storage/{s3,fake}/**` |
| modules:kakao | `map/infra/client/{kakao,fake}/**`, `user/infra/security/oauth/{helper/KakaoOauthHelper,oidc/KakaoOidc}.kt` |
| modules:apple | `user/infra/security/oauth/{helper/AppleOauthHelper,oidc/AppleOidc}.kt` |
| modules:discord | Discord 발신 어댑터, `notification/properties/DiscordProperties.kt`, `notification/infra/config/NotificationConfig.kt` |
| bootstrap | `NekiApplication.kt`, `common/config/{JasyptConfig,AsyncConfig}.kt`, `common/infra/config/RestClientConfig.kt`, `resources/**`(migration 제외) |

> 정확한 파일 목록은 각 Task의 `git mv` 글롭으로 지정한다. 실행 전 `git mv` 대상이 비어있지 않은지 `ls`로 확인한다.

---

## Task 1: Gradle 멀티 모듈 스캐폴딩 (빈 모듈 + 루트 공통 설정)

**Files:**
- Modify: `settings.gradle.kts`
- Create: `build.gradle.kts` (루트를 aggregator로 재작성 — 기존 내용은 Task 2에서 bootstrap으로 이동)
- Create: 각 모듈 디렉토리 + `build.gradle.kts` (빈 스텁)

- [ ] **Step 1: settings.gradle.kts에 모듈 등록**

`settings.gradle.kts`:
```kotlin
rootProject.name = "Neki"

include(
    ":core",
    ":domain",
    ":application",
    ":bootstrap",
    ":modules:postgres",
    ":modules:redis",
    ":modules:s3",
    ":modules:kakao",
    ":modules:apple",
    ":modules:discord",
)
```

- [ ] **Step 2: 기존 루트 build.gradle.kts 백업**

Run:
```bash
git mv build.gradle.kts build.gradle.kts.bak
```
(Task 2에서 내용을 bootstrap으로 옮긴 뒤 삭제)

- [ ] **Step 3: 루트 build.gradle.kts를 aggregator + subprojects 공통 설정으로 작성**

`build.gradle.kts`:
```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.10"
    val spotlessVersion = "6.25.0"

    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    kotlin("kapt") version kotlinVersion apply false
    id("com.diffplug.spotless") version spotlessVersion apply false
}

allprojects {
    group = "com.neki"
    version = "1.0.0"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    val ktlintVersion = "1.5.0"

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    dependencies {
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("io.kotest:kotest-runner-junit5:5.9.1")
        "testImplementation"("io.kotest:kotest-assertions-core:5.9.1")
        "testImplementation"("io.kotest.extensions:kotest-extensions-spring:1.3.0")
        "testImplementation"("io.mockk:mockk:1.13.10")
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion).editorConfigOverride(
                mapOf(
                    "max_line_length" to "120",
                    "indent_size" to "4",
                    "insert_final_newline" to "true",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                ),
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion)
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> { useJUnitPlatform() }
}
```

- [ ] **Step 4: 모든 모듈 디렉토리 + 빈 build.gradle.kts 생성**

Run:
```bash
for m in core domain application bootstrap modules/postgres modules/redis modules/s3 modules/kakao modules/apple modules/discord; do
  mkdir -p "$m/src/main/kotlin/com/neki" "$m/src/main/resources"
  printf 'dependencies {\n}\n' > "$m/build.gradle.kts"
done
ls -d core domain application bootstrap modules/*
```
Expected: 10개 모듈 디렉토리 출력.

- [ ] **Step 5: 빈 구성 빌드 검증 (소스 없어 컴파일 대상 없음)**

Run: `./gradlew projects`
Expected: 루트 하위에 `:core`, `:domain`, `:application`, `:bootstrap`, `:modules:postgres` 등이 트리로 표시. 실패 없음.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build: multi-module Gradle 스캐폴딩 (빈 모듈 + 루트 공통 설정)"
```

---

## Task 2: 전체 소스를 :bootstrap 모듈로 이동 (단일 모듈 상태로 green 유지)

**Files:**
- Move: `src/**` → `bootstrap/src/**`
- Modify: `bootstrap/build.gradle.kts` (기존 루트 의존성 전부 이전)
- Delete: `build.gradle.kts.bak`

- [ ] **Step 1: src 전체를 bootstrap으로 이동**

Run:
```bash
git mv src bootstrap/src
ls bootstrap/src/main/kotlin/com/neki
```
Expected: `NekiApplication.kt`, `auth`/`common`/`map`/`media`/`photo`/`pose`/`support`/`user`/`notification` 등 출력.

- [ ] **Step 2: bootstrap/build.gradle.kts에 기존 루트 의존성 전부 작성**

`bootstrap/build.gradle.kts` (기존 `build.gradle.kts.bak`의 dependencies/플러그인/태스크를 이전; 공통 부분은 루트 subprojects가 이미 제공하므로 spring-boot/jpa/kapt 플러그인 + 모듈 고유 의존성만):
```kotlin
plugins {
    id("org.springframework.boot")
    kotlin("plugin.jpa")
    kotlin("kapt")
    idea
}

val jwtVersion = "0.12.5"
val nimbusVersion = "9.37.3"
val bouncyCastleVersion = "1.78"
val awsSdkVersion = "2.27.0"
val springDocVersion = "2.6.0"
val jasyptVersion = "3.0.5"
val logstashEncoderVersion = "8.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

idea {
    module {
        sourceDirs.add(file("build/generated/source/kapt/main"))
        generatedSourceDirs.add(file("build/generated/source/kapt/main"))
    }
}

tasks.processResources {
    filesMatching("application.yaml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf("version" to project.version.toString()),
        )
    }
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
```

- [ ] **Step 3: 백업 삭제**

Run: `rm build.gradle.kts.bak`

- [ ] **Step 4: 빌드 검증 (단일 모듈 상태)**

Run: `./gradlew :bootstrap:build`
Expected: BUILD SUCCESSFUL. 모든 기존 테스트(e2e 포함) 통과. (kapt Q클래스 생성도 bootstrap에서 정상 동작)

- [ ] **Step 5: 앱 기동 스모크 (선택, 로컬 Docker 필요)**

Run: `docker compose up -d && ./gradlew :bootstrap:bootRun` (수동 확인 후 Ctrl+C)
Expected: 정상 기동 로그.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: 전체 소스를 :bootstrap 모듈로 이동 (단일 모듈 유지)"
```

---

## Task 3: :core 추출

**Files:**
- Move: 아래 글롭 → `core/src/main/kotlin/com/neki/common/...` (패키지 경로 유지)
- Modify: `core/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: core 파일 이동 (패키지 경로 보존)**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
C=core/src/main/kotlin/com/neki
mkdir -p $C/common/api/dto $C/common/exception/dto $C/common/domain/vo $C/common/annotation $C/common/transaction
git mv $B/common/api/dto/ResultCode.kt        $C/common/api/dto/ResultCode.kt
git mv $B/common/api/dto/BaseResponse.kt      $C/common/api/dto/BaseResponse.kt
git mv $B/common/exception/BusinessException.kt $C/common/exception/BusinessException.kt
git mv $B/common/exception/dto/ExceptionDto.kt  $C/common/exception/dto/ExceptionDto.kt
git mv $B/common/domain/vo/SortOrder.kt       $C/common/domain/vo/SortOrder.kt
git mv $B/common/domain/BaseTimeEntity.kt     $C/common/domain/BaseTimeEntity.kt
git mv $B/common/annotation/UseCase.kt        $C/common/annotation/UseCase.kt
git mv $B/common/transaction/TransactionRunner.kt $C/common/transaction/TransactionRunner.kt
```

- [ ] **Step 2: core/build.gradle.kts 작성 (어노테이션 의존만, 인프라 연결 없음)**

`core/build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.springframework.data:spring-data-jpa")
    implementation("io.swagger.core.v3:swagger-annotations")
}
```
> 검증 노트: `swagger-annotations`와 `spring-data-jpa` 버전은 Spring Boot BOM이 관리한다. 만약 `swagger-annotations`가 BOM에서 미관리로 컴파일 실패 시, springdoc 2.6.0이 사용하는 버전(`2.2.22`)으로 명시: `implementation("io.swagger.core.v3:swagger-annotations:2.2.22")`.

- [ ] **Step 3: bootstrap이 :core에 의존하도록 추가**

`bootstrap/build.gradle.kts`의 `dependencies {}` 최상단에 추가:
```kotlin
    implementation(project(":core"))
```

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :core:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL. (패키지명 유지로 bootstrap 측 import 변경 불필요)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 공유 커널을 :core 모듈로 추출"
```

---

## Task 4: :domain 추출 (+ QueryDSL kapt를 domain으로)

**Files:**
- Move: `*/domain/**` (7개 도메인) → `domain/src/main/kotlin/com/neki/<도메인>/domain/**`
- Modify: `domain/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: 각 도메인의 domain 패키지 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
D=domain/src/main/kotlin/com/neki
for dom in photo user pose map media support notification; do
  if [ -d "$B/$dom/domain" ]; then
    mkdir -p "$D/$dom"
    git mv "$B/$dom/domain" "$D/$dom/domain"
  fi
done
find $D -name "*.kt" | head
```
Expected: 각 도메인의 entity/enums 파일들이 domain 모듈로 이동.

- [ ] **Step 2: domain/build.gradle.kts 작성 (core 의존 + QueryDSL kapt)**

`domain/build.gradle.kts`:
```kotlin
plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
}
```
> 근거: QueryDSL Q클래스는 `@Entity`가 있는 모듈(domain)에서 kapt로 생성되어야 `:modules:postgres`가 의존을 통해 사용할 수 있다. PostGIS 좌표 타입(JTS) 엔티티가 있으므로 hibernate-spatial/jts도 domain에 필요.
>
> **Task 3에서 확정된 사항:** `BaseTimeEntity`(@MappedSuperclass)는 `:core`에 있고, 그 Q클래스 `QBaseTimeEntity`는 이미 `:core`의 kapt가 생성한다(core build.gradle.kts에 querydsl-jpa+kapt 존재). domain은 `:core`에 의존하므로 엔티티 Q클래스(QFolder 등) 생성 시 core의 `com.neki.common.domain.QBaseTimeEntity`를 참조한다. **검증 포인트:** domain의 kapt가 `QBaseTimeEntity`를 중복 생성하거나(같은 FQN 충돌) 또는 못 찾는지 빌드로 확인할 것. 충돌 시에는 domain에서 BaseTimeEntity를 재처리하지 않도록(이미 core가 제공) 처리. 이 검증을 Task 4 초반에 수행한다.

- [ ] **Step 3: bootstrap이 :domain에 의존하도록 추가**

`bootstrap/build.gradle.kts` `dependencies {}`에 추가:
```kotlin
    implementation(project(":domain"))
```

- [ ] **Step 4: 빌드 검증 (domain의 Q클래스 생성 확인)**

Run: `./gradlew :domain:kaptKotlin :domain:compileKotlin`
Expected: BUILD SUCCESSFUL. `domain/build/generated/source/kapt/main`에 `Q*` 클래스 생성 확인.

Run: `./gradlew :bootstrap:build`
Expected: BUILD SUCCESSFUL. (bootstrap의 QueryDSL 리포지토리가 domain의 Q클래스를 참조 — 아직 bootstrap에 querydsl-apt가 있어도 무방하나, 중복 생성 방지를 위해 Task 6에서 정리)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 도메인 엔티티를 :domain 모듈로 추출 (QueryDSL kapt 포함)"
```

---

## Task 5: :application 추출 (api + usecase + 웹 보안 기계장치 + 공통 웹 설정)

**Files:**
- Move: 아래 글롭 → `application/src/main/kotlin/com/neki/...`
- Modify: `application/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: api + application 레이어 이동 (7개 도메인)**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
A=application/src/main/kotlin/com/neki
for dom in photo user pose map media support notification auth; do
  for layer in api application; do
    if [ -d "$B/$dom/$layer" ]; then
      mkdir -p "$A/$dom"
      git mv "$B/$dom/$layer" "$A/$dom/$layer"
    fi
  done
done
```

- [ ] **Step 2: Spring Security 기계장치 + OAuth 추상화 + 내부 클라이언트 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
A=application/src/main/kotlin/com/neki
mkdir -p $A/user/infra/security/oauth
# 웹 보안 기계장치
git mv $B/user/infra/security/config/SecurityConfig.kt $A/user/infra/security/config/SecurityConfig.kt 2>/dev/null
git mv $B/user/infra/security/filter   $A/user/infra/security/filter
git mv $B/user/infra/security/handler  $A/user/infra/security/handler
git mv $B/user/infra/security/token    $A/user/infra/security/token
git mv $B/user/infra/security/config/OauthProperties.kt $A/user/infra/security/config/OauthProperties.kt 2>/dev/null
# OAuth 추상화(인터페이스/레지스트리/검증) — 구현체(Kakao/Apple)는 제외하고 이동
git mv $B/user/infra/security/oauth/Oidc*.kt        $A/user/infra/security/oauth/ 2>/dev/null
git mv $B/user/infra/security/oauth/registry        $A/user/infra/security/oauth/registry 2>/dev/null
mkdir -p $A/user/infra/security/oauth/helper $A/user/infra/security/oauth/oidc
git mv $B/user/infra/security/oauth/helper/OauthHelper.kt $A/user/infra/security/oauth/helper/OauthHelper.kt 2>/dev/null
git mv $B/user/infra/security/oauth/oidc/Oidc.kt         $A/user/infra/security/oauth/oidc/Oidc.kt 2>/dev/null
# 내부 크로스 도메인 클라이언트/이벤트/제너레이터
git mv $B/user/infra/client    $A/user/infra/client 2>/dev/null
git mv $B/user/infra/event     $A/user/infra/event 2>/dev/null
git mv $B/user/infra/generator $A/user/infra/generator 2>/dev/null
# notification 이벤트 리스너 (외부 발신 어댑터는 Task 11에서 modules:discord로)
git mv $B/notification/infra/discord/UserDiscordListener.kt $A/notification/infra/discord/UserDiscordListener.kt 2>/dev/null
```
> 주의: `OidcRegistry`/`OauthHelperRegistry`/`OidcTokenValidator`는 `oauth/registry`·`oauth/` 글롭에 포함된다. Kakao/Apple **구현체**(`KakaoOidc`,`AppleOidc`,`KakaoOauthHelper`,`AppleOauthHelper`)는 이동하지 않고 bootstrap에 남겨 Task 9/10에서 modules로 보낸다. 이동 후 `ls $B/user/infra/security/oauth/oidc $B/user/infra/security/oauth/helper`로 구현체만 남았는지 확인.

- [ ] **Step 3: 공통 웹 설정 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
A=application/src/main/kotlin/com/neki
mkdir -p $A/common
git mv $B/common/api          $A/common/api
git mv $B/common/exception/handler $A/common/exception/handler
git mv $B/common/filter       $A/common/filter
mkdir -p $A/common/properties
git mv $B/common/properties/AppProperties.kt $A/common/properties/AppProperties.kt
```
> `common/exception/{BusinessException,dto}`는 이미 core로 이동됨. `common/api/dto/{ResultCode,BaseResponse}`도 core. 여기서 이동하는 `common/api`는 `api/config`(ObjectMapper/Web)와 `api/document`(Swagger/RequiresSecurity)만 남은 상태.

- [ ] **Step 4: application/build.gradle.kts 작성**

`application/build.gradle.kts`:
```kotlin
plugins {
    kotlin("plugin.jpa")
}

val jwtVersion = "0.12.5"
val nimbusVersion = "9.37.3"
val springDocVersion = "2.6.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework.web:spring-web")
}
```
> `spring-data-jpa`는 usecase가 `BaseTimeEntity`/엔티티·트랜잭션을 다루기 위해 필요(연결은 bootstrap·postgres가 담당). 컴파일 에러로 누락 의존성 발견 시 추가.

- [ ] **Step 5: bootstrap이 :application에 의존하도록 추가**

`bootstrap/build.gradle.kts` `dependencies {}`에 추가:
```kotlin
    implementation(project(":application"))
```

- [ ] **Step 6: 빌드 검증**

Run: `./gradlew :application:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL. 컴파일 에러 발생 시 application/build.gradle.kts에 누락 의존성 추가 후 재시도.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: api/usecase/웹보안/공통웹설정을 :application 모듈로 추출"
```

---

## Task 6: :modules:postgres 추출 (영속성 어댑터 + QueryDSL repo + Flyway)

**Files:**
- Move: `*/infra/persist/**`, `common/infra/config/{QueryDslConfig,JpaAuditingConfig}.kt`, `resources/db/migration/**`
- Modify: `modules/postgres/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: 영속성 어댑터 + JPA 설정 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
P=modules/postgres/src/main/kotlin/com/neki
for dom in photo user pose map media support notification; do
  if [ -d "$B/$dom/infra/persist" ]; then
    mkdir -p "$P/$dom/infra"
    git mv "$B/$dom/infra/persist" "$P/$dom/infra/persist"
  fi
done
mkdir -p $P/common/infra/config
git mv $B/common/infra/config/QueryDslConfig.kt   $P/common/infra/config/QueryDslConfig.kt
git mv $B/common/infra/config/JpaAuditingConfig.kt $P/common/infra/config/JpaAuditingConfig.kt
```

- [ ] **Step 2: Flyway 마이그레이션을 postgres 모듈 resources로 이동**

Run:
```bash
mkdir -p modules/postgres/src/main/resources/db
git mv bootstrap/src/main/resources/db/migration modules/postgres/src/main/resources/db/migration
ls modules/postgres/src/main/resources/db/migration | head
```
Expected: V1~V18 SQL 파일 출력.

- [ ] **Step 3: modules/postgres/build.gradle.kts 작성**

`modules/postgres/build.gradle.kts`:
```kotlin
plugins {
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
}
```
> QueryDSL Q클래스는 `:domain`이 생성·노출하므로 여기서 `kapt`는 불필요(querydsl-jpa 런타임/컴파일 의존만). 

- [ ] **Step 4: bootstrap에서 querydsl kapt 제거 + :modules:postgres 의존 추가**

`bootstrap/build.gradle.kts`:
- `kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")` 라인 삭제
- `kotlin("kapt")` 플러그인 라인 삭제 (bootstrap에 더 이상 kapt 불필요)
- `kotlin("plugin.jpa")` 도 bootstrap에서 직접 엔티티를 다루지 않으면 삭제 가능(엔티티 없음 → 삭제)
- `idea { ... kapt ... }` 블록 삭제
- `dependencies {}`에 추가:
```kotlin
    implementation(project(":modules:postgres"))
```
- bootstrap에 남은 JPA/querydsl/postgres/flyway/hibernate-spatial/jts 직접 의존 라인 삭제(이제 :modules:postgres가 제공)

- [ ] **Step 5: 빌드 검증**

Run: `./gradlew :modules:postgres:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL. Flyway 마이그레이션이 postgres 모듈 classpath에서 로드됨.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: JPA 영속성/QueryDSL/Flyway를 :modules:postgres로 추출"
```

---

## Task 7: :modules:redis 추출

> **실행 중 확정:** Redis 사용처가 user뿐 아니라 media/pose/lock에도 있음이 발견됨. 전 도메인의 Redis 어댑터를 :modules:redis로 **완전 통합**한다 — `common/config/RedisCacheConfig.kt`, `user/infra/cache/**`, `media/infra/cache/**`, `pose/infra/cache/**`, `media/infra/lock/**`(RedisDistributedLockAdapter + DistributedLockProperties + fake). 완료 후 bootstrap MAIN은 redis starter 미보유(테스트가 참조하면 testImplementation만 임시 유지, Task 13에서 제거).

**Files:**
- Move: `common/config/RedisCacheConfig.kt`, `{user,media,pose}/infra/cache/**`, `media/infra/lock/**`
- Modify: `modules/redis/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: Redis 관련 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
R=modules/redis/src/main/kotlin/com/neki
mkdir -p $R/common/config $R/user/infra
git mv $B/common/config/RedisCacheConfig.kt $R/common/config/RedisCacheConfig.kt
git mv $B/user/infra/cache $R/user/infra/cache
```

- [ ] **Step 2: modules/redis/build.gradle.kts 작성**

`modules/redis/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
```

- [ ] **Step 3: bootstrap 의존 추가 + 직접 redis 의존 제거**

`bootstrap/build.gradle.kts`: `spring-boot-starter-data-redis` 직접 의존 삭제, `implementation(project(":modules:redis"))` 추가.

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :modules:redis:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: Redis 캐시를 :modules:redis로 추출"
```

---

## Task 8: :modules:s3 추출

**Files:**
- Move: `media/infra/storage/**`
- Modify: `modules/s3/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: S3 스토리지 어댑터 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
S=modules/s3/src/main/kotlin/com/neki
mkdir -p $S/media/infra
git mv $B/media/infra/storage $S/media/infra/storage
```

- [ ] **Step 2: modules/s3/build.gradle.kts 작성**

`modules/s3/build.gradle.kts`:
```kotlin
val awsSdkVersion = "2.27.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")
}
```

- [ ] **Step 3: bootstrap 의존 추가 + 직접 aws 의존 제거**

`bootstrap/build.gradle.kts`: `software.amazon.awssdk:*` 직접 의존 삭제, `implementation(project(":modules:s3"))` 추가.

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :modules:s3:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: S3 스토리지를 :modules:s3로 추출"
```

---

## Task 9: :modules:kakao 추출 (Map API + Kakao OIDC)

**Files:**
- Move: `map/infra/client/{kakao,fake}/**`, `user/infra/security/oauth/{helper/KakaoOauthHelper,oidc/KakaoOidc}.kt`
- Modify: `modules/kakao/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: Kakao 관련 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
K=modules/kakao/src/main/kotlin/com/neki
mkdir -p $K/map/infra/client $K/user/infra/security/oauth/helper $K/user/infra/security/oauth/oidc
git mv $B/map/infra/client/kakao $K/map/infra/client/kakao
git mv $B/map/infra/client/fake  $K/map/infra/client/fake 2>/dev/null
git mv $B/user/infra/security/oauth/helper/KakaoOauthHelper.kt $K/user/infra/security/oauth/helper/KakaoOauthHelper.kt
git mv $B/user/infra/security/oauth/oidc/KakaoOidc.kt          $K/user/infra/security/oauth/oidc/KakaoOidc.kt
```

- [ ] **Step 2: modules/kakao/build.gradle.kts 작성**

`modules/kakao/build.gradle.kts`:
```kotlin
val nimbusVersion = "9.37.3"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework:spring-web")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
}
```
> OIDC 검증에 nimbus가 필요하면 포함. 컴파일 에러로 불필요 판명 시 제거.

- [ ] **Step 3: bootstrap 의존 추가**

`bootstrap/build.gradle.kts`: `implementation(project(":modules:kakao"))` 추가.

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :modules:kakao:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: Kakao Map/OIDC 클라이언트를 :modules:kakao로 추출"
```

---

## Task 10: :modules:apple 추출 (Apple OIDC)

**Files:**
- Move: `user/infra/security/oauth/{helper/AppleOauthHelper,oidc/AppleOidc}.kt`
- Modify: `modules/apple/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: Apple OIDC 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
AP=modules/apple/src/main/kotlin/com/neki
mkdir -p $AP/user/infra/security/oauth/helper $AP/user/infra/security/oauth/oidc
git mv $B/user/infra/security/oauth/helper/AppleOauthHelper.kt $AP/user/infra/security/oauth/helper/AppleOauthHelper.kt
git mv $B/user/infra/security/oauth/oidc/AppleOidc.kt          $AP/user/infra/security/oauth/oidc/AppleOidc.kt
# 남은 oauth 빈 디렉토리 확인
find $B/user/infra/security/oauth -type f
```
Expected: 마지막 find는 출력 없음(구현체 모두 이동 완료).

- [ ] **Step 2: modules/apple/build.gradle.kts 작성**

`modules/apple/build.gradle.kts`:
```kotlin
val nimbusVersion = "9.37.3"
val bouncyCastleVersion = "1.78"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework:spring-web")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
}
```
> Apple OIDC는 JWK/ES256 검증에 nimbus + BouncyCastle을 사용. 실제 사용 의존성에 맞춰 컴파일 에러 시 조정.

- [ ] **Step 3: bootstrap 의존 추가 + BouncyCastle 직접 의존 제거**

`bootstrap/build.gradle.kts`: `org.bouncycastle:*`·`com.nimbusds:nimbus-jose-jwt` 직접 의존이 bootstrap에서 더 불필요하면 삭제(kakao/apple 모듈이 제공), `implementation(project(":modules:apple"))` 추가.

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :modules:apple:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: Apple OIDC 클라이언트를 :modules:apple로 추출"
```

---

## Task 11: :modules:discord 추출

**Files:**
- Move: Discord 외부 발신 어댑터 + `notification/properties/DiscordProperties.kt` + `notification/infra/config/NotificationConfig.kt`
- Modify: `modules/discord/build.gradle.kts`, `bootstrap/build.gradle.kts`

- [ ] **Step 1: 남은 notification/infra 구성 확인 후 이동**

Run:
```bash
B=bootstrap/src/main/kotlin/com/neki
DC=modules/discord/src/main/kotlin/com/neki
find $B/notification -type f
mkdir -p $DC/notification/infra/config $DC/notification/properties
# UserDiscordListener는 Task 5에서 application으로 이동됨. 외부 발신 어댑터/설정/프로퍼티만 이동.
git mv $B/notification/infra/config/NotificationConfig.kt $DC/notification/infra/config/NotificationConfig.kt 2>/dev/null
git mv $B/notification/properties $DC/notification/properties 2>/dev/null
# 외부 발신 어댑터(HTTP/webhook) 파일이 notification/infra 하위에 더 있으면 함께 이동
```
> `find` 결과를 보고 외부 발신(HTTP) 어댑터 파일을 추가로 `git mv`. 만약 `NotificationConfig`가 RestClient 등 외부 호출 빈만 정의한다면 discord로, 순수 이벤트 설정이면 application으로 판단.

- [ ] **Step 2: modules/discord/build.gradle.kts 작성**

`modules/discord/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework:spring-web")
}
```

- [ ] **Step 3: bootstrap 의존 추가**

`bootstrap/build.gradle.kts`: `implementation(project(":modules:discord"))` 추가.

- [ ] **Step 4: 빌드 검증**

Run: `./gradlew :modules:discord:compileKotlin :bootstrap:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: Discord 웹훅을 :modules:discord로 추출"
```

---

## Task 12: :bootstrap 정리 (실행/조립 전용 확인)

**Files:**
- Verify: `bootstrap/src/main/kotlin/com/neki`에 `NekiApplication.kt` + `common/config/{JasyptConfig,AsyncConfig}.kt` + `common/infra/config/RestClientConfig.kt`만 남아야 함
- Modify: `bootstrap/build.gradle.kts` (잔여 의존성 정리)

- [ ] **Step 1: bootstrap 잔여 코드 확인**

Run:
```bash
find bootstrap/src/main/kotlin -name "*.kt" | sort
```
Expected: 다음만 남음 —
`NekiApplication.kt`, `common/config/JasyptConfig.kt`, `common/config/AsyncConfig.kt`, `common/infra/config/RestClientConfig.kt`.
그 외 도메인/공통 파일이 남아 있으면 적절한 모듈로 추가 `git mv` 후 해당 모듈 의존 추가.

- [ ] **Step 2: bootstrap/build.gradle.kts 최종 정리**

`bootstrap/build.gradle.kts` 최종 형태(모듈 의존 + 실행에 필요한 starter만):
```kotlin
plugins {
    id("org.springframework.boot")
    idea
}

val springDocVersion = "2.6.0"
val jasyptVersion = "3.0.5"
val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:s3"))
    implementation(project(":modules:kakao"))
    implementation(project(":modules:apple"))
    implementation(project(":modules:discord"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.processResources {
    filesMatching("application.yaml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf("version" to project.version.toString()),
        )
    }
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
```
> `web`/`security`/`actuator` starter는 bootstrap의 자동설정(필터체인 활성화 등)을 위해 유지. 실제로 application 모듈 의존을 통해 전이될 수 있으나, 자동설정 트리거 명시를 위해 bootstrap에도 선언.

- [ ] **Step 3: 전체 빌드 검증**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. 전 모듈 컴파일 + 테스트 통과.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: :bootstrap을 실행/조립 전용으로 정리"
```

---

## Task 13: 테스트 재배치 + ArchUnit 모듈 규칙

**Files:**
- Move: 단위 테스트는 해당 모듈로, e2e/통합 테스트는 bootstrap에 유지
- Modify: ArchUnit 규칙 (`com/neki/rule`)

- [ ] **Step 1: 도메인 단위 테스트(usecase 테스트)를 application 모듈로 이동**

Run:
```bash
BT=bootstrap/src/test/kotlin/com/neki
AT=application/src/test/kotlin/com/neki
for dom in photo user pose map media support; do
  if [ -d "$BT/$dom/application" ]; then
    mkdir -p "$AT/$dom"
    git mv "$BT/$dom/application" "$AT/$dom/application"
  fi
done
# testfixture는 여러 모듈에서 공유 → application으로 이동(혹은 별도 test-fixtures 검토)
git mv $BT/testfixture $AT/testfixture 2>/dev/null
```
> `testfixture`가 여러 모듈 테스트에서 쓰이면 Gradle `java-test-fixtures` 플러그인 도입을 검토(미해결 항목). POC에서는 application에 두고 의존으로 공유.

- [ ] **Step 2: 인프라 단위 테스트 이동**

Run:
```bash
BT=bootstrap/src/test/kotlin/com/neki
# media 인프라 테스트(cache/lock) → 해당 인프라 모듈
git mv $BT/media/infra modules/s3/src/test/kotlin/com/neki/media/infra 2>/dev/null || \
  (mkdir -p modules/s3/src/test/kotlin/com/neki/media && git mv $BT/media/infra modules/s3/src/test/kotlin/com/neki/media/infra)
# user security filter 테스트 → application
mkdir -p application/src/test/kotlin/com/neki/user/infra
git mv $BT/user/infra application/src/test/kotlin/com/neki/user/infra 2>/dev/null
# common 테스트(api/config, filter) → application
mkdir -p application/src/test/kotlin/com/neki/common
git mv $BT/common application/src/test/kotlin/com/neki/common 2>/dev/null
```
> `find bootstrap/src/test -type f`로 잔여 테스트 위치를 확인하고, 컴파일 에러 시 모듈 의존(`testImplementation(project(...))`)을 추가하거나 해당 모듈로 이동.

- [ ] **Step 3: e2e + JasyptTest + ArchUnit rule은 bootstrap 유지**

`bootstrap/src/test/kotlin/com/neki`에 `e2e/**`, `JasyptTest.kt`, `NekiApplicationTests.kt`, `rule/**` 유지 (전체 컨텍스트가 필요하므로 bootstrap이 적합).

- [ ] **Step 4: ArchUnit 규칙을 모듈 의존 방향 검증으로 갱신**

`bootstrap/src/test/kotlin/com/neki/rule/` 의 ArchUnit 테스트를 모듈 경계 규칙으로 수정:
- core는 domain/application/modules/bootstrap 패키지를 참조하지 않는다.
- domain은 application/modules/bootstrap을 참조하지 않는다.
- application은 modules/bootstrap을 참조하지 않는다.
- 도메인 간 직접 참조 금지(`com.neki.<A>` ↛ `com.neki.<B>` domain 내부).

예시 규칙(기존 규칙 스타일에 맞춰 작성):
```kotlin
@AnalyzeClasses(packages = ["com.neki"])
class ModuleDependencyRulesTest {
    @ArchTest
    val coreMustNotDependOnOuterLayers =
        noClasses().that().resideInAPackage("com.neki.common.api.dto..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.neki..api..", "com.neki..infra..")
}
```
> 기존 `com/neki/rule`의 규칙 스타일·범위를 먼저 확인하고 일관되게 작성. 모듈 의존은 Gradle `project(...)` 그래프로도 강제되므로, ArchUnit은 패키지 레벨 보조 검증으로 둔다.

- [ ] **Step 5: 테스트 빌드 검증**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. 전 모듈 테스트 통과.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test: 테스트를 모듈별로 재배치하고 ArchUnit 모듈 의존 규칙 추가"
```

---

## Task 14: 모듈별 설정 yaml 분리 + bootstrap 통합

**Files:**
- Create: `modules/<name>/src/main/resources/application-<name>.yaml`
- Modify: bootstrap의 `application.yaml` (`spring.config.import`)

- [ ] **Step 1: 현재 application.yaml에서 의존성별 설정 식별**

Run: `sed -n '1,200p' bootstrap/src/main/resources/application.yaml`
Expected: datasource/jpa/flyway(→postgres), redis(→redis), aws/s3(→s3), kakao(→kakao), apple(→apple), discord(→discord) 블록 식별.

- [ ] **Step 2: 모듈별 yaml 생성 (예: redis)**

`modules/redis/src/main/resources/application-redis.yaml`:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```
> 실제 값은 Step 1에서 식별한 기존 application*.yaml의 해당 블록을 그대로 이전. postgres/s3/kakao/apple/discord도 동일 패턴으로 각 모듈 resources에 `application-<name>.yaml` 작성하고, 기존 yaml에서 해당 블록 제거.

- [ ] **Step 3: bootstrap application.yaml에서 모듈 설정 import**

`bootstrap/src/main/resources/application.yaml` 상단에 추가:
```yaml
spring:
  config:
    import:
      - classpath:application-postgres.yaml
      - classpath:application-redis.yaml
      - classpath:application-s3.yaml
      - classpath:application-kakao.yaml
      - classpath:application-apple.yaml
      - classpath:application-discord.yaml
```
> 각 모듈 jar가 classpath에 있으므로 `classpath:` import로 병합된다. 프로파일별(`application-local/staging/prod.yaml`) 오버라이드는 bootstrap에 유지.

- [ ] **Step 4: 기동 검증 (로컬)**

Run: `docker compose up -d && ./gradlew :bootstrap:bootRun`
Expected: 정상 기동, 설정 병합 확인(로그에 datasource/redis 연결 성공). 확인 후 종료.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "config: 의존성별 application-<name>.yaml 분리 및 bootstrap 통합"
```

---

## Task 15: 최종 전체 검증

- [ ] **Step 1: 클린 빌드**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL. 전 모듈 컴파일 + 전체 테스트(unit + e2e) 통과.

- [ ] **Step 2: spotless 적용**

Run: `./gradlew spotlessApply && ./gradlew spotlessCheck`
Expected: 통과.

- [ ] **Step 3: 모듈 의존 그래프 확인**

Run: `./gradlew :bootstrap:dependencies --configuration runtimeClasspath | grep "project :"`
Expected: bootstrap → core/domain/application/modules:* 의존 표시. core가 외부 모듈에 의존하지 않음 확인.

- [ ] **Step 4: bootJar 생성 검증**

Run: `./gradlew :bootstrap:bootJar && ls -la bootstrap/build/libs`
Expected: 실행 가능한 layered jar 생성.

- [ ] **Step 5: 앱 기동 + 핵심 API 스모크 (로컬)**

Run: `docker compose up -d && ./gradlew :bootstrap:bootRun`
Expected: 정상 기동. Swagger(`/swagger-ui`) 접근, 인증/포토 등 핵심 엔드포인트 1개씩 수동 호출 성공. 확인 후 종료.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build: 멀티 모듈 전환 최종 검증 통과"
```

---

## 미해결/구현 중 결정 사항 (스펙 §8 연계)

- `testfixture` 공유 방식: application 내 배치 vs Gradle `java-test-fixtures` 플러그인.
- `RestClientConfig` 공유 빈 위치 확정(현 계획: bootstrap). 각 외부 모듈이 자체 RestClient를 갖는 편이 결합도상 더 깔끔할 수 있음 — 빌드/런타임 확인 후 결정.
- 루트 빌드 공통화: 현 계획은 `subprojects {}`. 모듈이 더 늘면 `build-logic` convention plugin으로 전환 검토.
- ArchUnit 규칙 범위: Gradle 의존 그래프가 1차 강제, ArchUnit은 패키지 레벨 보조.
- `spring-data-jpa`를 core가 의존하는 것에 대한 순수성 재검토(완화 결정에 따라 허용했으나, 추후 `jakarta.persistence-api` + `spring-data-commons` 최소 조합으로 축소 가능).
```
