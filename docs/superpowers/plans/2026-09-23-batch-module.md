# apps/batch 실행 모듈 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Team-Neki-Notification 의 배치 앱 구조(스케줄러, 잡 런처, 수동 트리거 API, 기능 플래그, Flyway 소유 메타 테이블, 프로브)를 `apps/batch` 실행 모듈로 가져오고, 배선 검증용 no-op `sampleJob` 과 배포 배선까지 붙인다.

**Architecture:** `:apps:batch` 는 `:core`, `:domain`, `:modules:postgres`, `:modules:jasypt` 만 의존하는 두 번째 bootJar 다. 컴포넌트 스캔을 `com.neki.batch`, `com.neki.core`, `com.neki.config.{postgres,jasypt}` 로 좁히고, `:domain` 의 runtime classpath 로 딸려오는 Security/Redis 자동 설정을 제외한다. Spring Batch 메타 테이블은 `modules/postgres` 의 Flyway V31 이 `IF NOT EXISTS` 로 소유한다 (Notification 앱이 prod 에 이미 만든 동일 스키마 편입).

**Tech Stack:** Kotlin 2.0, Spring Boot 3.5.8, Spring Batch 5.2.4, JPA/H2(test), Flyway, Kotest + MockK, GitHub Actions.

**설계 문서:** `docs/superpowers/specs/2026-09-23-batch-module-design.md`

**티켓:** BACKEND-128

> **변경 이력:** 이 플랜은 상주 프로세스(스케줄러, 트리거 API, 프로브) 전제로 실행됐다. 같은 날 Prefect 가 k8s Job 으로 띄우는 one-shot 프로세스로 방향을 바꾸면서 Task 2, 3 의 산출물(`SchedulingConfig`, `BatchJobLauncher`, `BatchJobController`, `SampleJobScheduler`)과 Task 5 의 워크플로 분기는 제거됐다. 현행 설계는 스펙 문서를 기준으로 한다. 메타 테이블 마이그레이션은 V10 이 아니라 V31 이다.

---

## 파일 맵

| 파일 | 책임 |
|------|------|
| `settings.gradle.kts` | `:apps:batch` include |
| `apps/batch/build.gradle.kts` | 모듈 의존성, bootJar 만 생성 |
| `apps/batch/src/main/kotlin/com/neki/batch/NekiBatchApplication.kt` | 스캔 범위, 자동 설정 제외, 엔티티/리포지터리 스캔 |
| `apps/batch/src/main/kotlin/com/neki/batch/common/config/SchedulingConfig.kt` | `@EnableScheduling` + 타임존 `Clock` |
| `apps/batch/src/main/kotlin/com/neki/batch/common/job/BatchJobLauncher.kt` | 잡 기동 공용 (파라미터 구성, 중복 기동 방어, 이름 기동) |
| `apps/batch/src/main/kotlin/com/neki/batch/common/api/controller/BatchJobController.kt` | 수동 트리거 API (`neki.batch.trigger-api-enabled` 게이트) |
| `apps/batch/src/main/kotlin/com/neki/batch/common/api/dto/BatchJobResponse.kt` | 트리거 API 응답 DTO |
| `apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobConfig.kt` | no-op `sampleJob` |
| `apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobScheduler.kt` | cron 스케줄러 (`neki.batch.scheduling-enabled` 게이트) |
| `apps/batch/src/main/resources/application.yaml` | 설정, 프로브, 기능 플래그 |
| `apps/batch/src/main/resources/logback-spring.xml` | api 와 동일한 로그 포맷 (`application=neki-batch`) |
| `apps/batch/src/test/resources/application-test.yml` | H2, Flyway off, 메타 테이블 auto-DDL |
| `apps/batch/src/test/kotlin/com/neki/batch/NekiBatchApplicationTest.kt` | 컨텍스트, 프로브 무인증, sampleJob 완료, 트리거 API |
| `apps/batch/src/test/kotlin/com/neki/batch/common/job/BatchJobLauncherTest.kt` | 런처 단위 테스트 (MockK) |
| `modules/postgres/src/main/resources/db/migration/V31__create_spring_batch_meta_tables.sql` | Spring Batch 5.2.4 메타 테이블 (`IF NOT EXISTS`) |
| `Dockerfile` | `ARG APP_MODULE=api` |
| `.github/workflows/deploy-staging.yml`, `deploy-prod.yml` | `app` 입력으로 모듈/이미지/GitOps 경로 분기 |
| `Makefile`, `README.md`, `.claude/CLAUDE.md`, `.claude/skills/architecture/SKILL.md`, `docs/layering-policy.md` | `apps/batch` 반영, `bootRun` 을 `:apps:api:bootRun` 으로 한정 |

---

### Task 1: Gradle 모듈과 앱 클래스

**Files:**
- Modify: `settings.gradle.kts`
- Create: `apps/batch/build.gradle.kts`
- Create: `apps/batch/src/main/kotlin/com/neki/batch/NekiBatchApplication.kt`
- Create: `apps/batch/src/main/resources/application.yaml`
- Create: `apps/batch/src/main/resources/logback-spring.xml`
- Create: `apps/batch/src/test/resources/application-test.yml`
- Test: `apps/batch/src/test/kotlin/com/neki/batch/NekiBatchApplicationTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성** (컨텍스트 기동 + 프로브 무인증)

```kotlin
package com.neki.batch

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NekiBatchApplicationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `liveness 프로브는 인증 없이 200 을 반환한다`() {
        val response: ResponseEntity<String> =
            restTemplate.getForEntity("/actuator/health/liveness", String::class.java)

        response.statusCode shouldBe HttpStatus.OK
    }
}
```

- [ ] **Step 2: settings.gradle.kts 에 모듈 추가**

`":apps:api",` 아래에 `":apps:batch",` 추가.

- [ ] **Step 3: build.gradle.kts 작성**

```kotlin
plugins {
    id("org.springframework.boot")
}

val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
    // k8s 프로브(/actuator/health)와 수동 트리거 API 를 위한 최소 web 스택.
    // actuator 는 NekiBatchApplication 이 ManagementWebSecurityAutoConfiguration 을 참조하므로 컴파일 의존이다
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    testRuntimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
```

- [ ] **Step 4: NekiBatchApplication 작성**

```kotlin
package com.neki.batch

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// 스캔 범위를 필요한 것만으로 좁힌다. com.neki 전체를 스캔하면 :domain 의 user/infra/security 가
// apps/api 에만 있는 CorsConfigurationSource 를 요구하고, modules 의 aws/redis/firebase 연결 설정이
// 각자의 yaml 과 외부 시스템을 요구해 기동이 깨진다.
// 실제 잡이 도메인 서비스/어댑터를 쓰게 되면 그 도메인 패키지(e.g. com.neki.domain.photo)를 여기에 추가한다.
@SpringBootApplication(
    scanBasePackages = ["com.neki.batch", "com.neki.core", "com.neki.config.postgres", "com.neki.config.jasypt"],
    exclude = [
        // :domain 의 spring-security 가 runtime classpath 에 전이로 올라와 기본 보안 체인이 프로브를 401 로 막는다
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        // application-redis.yaml 을 import 하지 않으므로 localhost 로 향하는 커넥션 팩토리를 만들지 않는다
        RedisAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
    ],
)
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.domain")
class NekiBatchApplication

fun main(args: Array<String>) {
    runApplication<NekiBatchApplication>(*args)
}
```

- [ ] **Step 5: application.yaml 작성**

```yaml
spring:
  application:
    name: neki-batch
  profiles:
    active: local
  config:
    import:
      - classpath:application-postgres.yaml
      - classpath:application-jasypt.yaml
  batch:
    job:
      # 기동 시 모든 Job 자동 실행 금지. 잡은 스케줄러/트리거 API 가 BatchJobLauncher 로 기동한다
      enabled: false
    jdbc:
      # 메타 테이블(BATCH_*)은 modules/postgres 의 Flyway V31 이 소유한다. 부팅 시 auto-DDL 금지
      initialize-schema: never
  jpa:
    open-in-view: false

# k8s preStop + terminationGracePeriodSeconds 와 정합하는 graceful shutdown
server:
  shutdown: graceful

# k8s 프로브: /actuator/health/liveness, /actuator/health/readiness
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
      group:
        readiness:
          include: readinessState,db

neki:
  batch:
    # Clock 과 @Scheduled cron 이 함께 쓰는 운영 타임존
    zone: Asia/Seoul
    # 스케줄러(@Scheduled) 빈 등록. 실제 잡이 생기면 운영에서 켠다
    scheduling-enabled: false
    # 수동 트리거 API(BatchJobController) 빈 등록. 인증 없음. pod 내부 전용, Service/Ingress 노출 금지
    trigger-api-enabled: false
    cron:
      sample: "0 0 4 * * *"
---
spring:
  config:
    activate:
      on-profile: local
neki:
  batch:
    trigger-api-enabled: true
---
spring:
  config:
    activate:
      on-profile: staging
neki:
  batch:
    trigger-api-enabled: true
```

- [ ] **Step 6: logback-spring.xml 복사**

`apps/api/src/main/resources/logback-spring.xml` 을 복사하고 `customFields` 의 `"application":"neki"` 를 `"application":"neki-batch"` 로 바꾼다.

- [ ] **Step 7: 테스트 설정 작성** (`apps/batch/src/test/resources/application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:batchtestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
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
  batch:
    jdbc:
      # Flyway 비활성이므로 메타 테이블은 Spring Batch 의 H2 스크립트로 만든다
      initialize-schema: always

neki:
  batch:
    trigger-api-enabled: true
```

- [ ] **Step 8: 테스트 실행**

Run: `./gradlew :apps:batch:test --tests 'com.neki.batch.NekiBatchApplicationTest'`
Expected: PASS

- [ ] **Step 9: 커밋**

```bash
git add settings.gradle.kts apps/batch
git commit -m "feat/BACKEND-128 apps/batch 실행 모듈 골격 추가"
```

---

### Task 2: 스케줄링 설정과 잡 런처

**Files:**
- Create: `apps/batch/src/main/kotlin/com/neki/batch/common/config/SchedulingConfig.kt`
- Create: `apps/batch/src/main/kotlin/com/neki/batch/common/job/BatchJobLauncher.kt`
- Test: `apps/batch/src/test/kotlin/com/neki/batch/common/job/BatchJobLauncherTest.kt`

- [ ] **Step 1: 실패하는 단위 테스트 작성**

```kotlin
package com.neki.batch.common.job

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class BatchJobLauncherTest {

    private val jobLauncher: JobLauncher = mockk()
    private val jobExplorer: JobExplorer = mockk()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val job: Job = mockk { every { name } returns "sampleJob" }
    private val launcher = BatchJobLauncher(jobLauncher, jobExplorer, clock, listOf(job))

    @Test
    fun `실행 중인 execution 이 있으면 기동하지 않고 null 을 반환한다`() {
        every { jobExplorer.findRunningJobExecutions("sampleJob") } returns setOf(mockk())

        launcher.launch(job).shouldBeNull()

        verify(exactly = 0) { jobLauncher.run(any(), any()) }
    }

    @Test
    fun `운영 타임존 오늘을 businessDate 로, 기동 시각을 launchedAt 으로 넘긴다`() {
        every { jobExplorer.findRunningJobExecutions("sampleJob") } returns emptySet()
        val params = slot<JobParameters>()
        val execution: JobExecution = mockk()
        every { jobLauncher.run(job, capture(params)) } returns execution

        launcher.launch(job) shouldBe execution

        params.captured.getString("businessDate") shouldBe "2026-09-24"
        params.captured.getLong("launchedAt") shouldBe clock.millis()
    }

    @Test
    fun `알 수 없는 잡 이름이면 IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> { launcher.launchByName("unknownJob") }
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :apps:batch:test --tests 'com.neki.batch.common.job.BatchJobLauncherTest'`
Expected: 컴파일 실패 (BatchJobLauncher 없음)

- [ ] **Step 3: SchedulingConfig 작성**

```kotlin
package com.neki.batch.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.ZoneId

@Configuration
@EnableScheduling
class SchedulingConfig {

    /** 운영 타임존 시계. businessDate 산출과 @Scheduled cron 의 zone 이 같은 값을 본다 */
    @Bean
    fun clock(@Value("\${neki.batch.zone:Asia/Seoul}") zone: String): Clock = Clock.system(ZoneId.of(zone))
}
```

- [ ] **Step 4: BatchJobLauncher 작성**

```kotlin
package com.neki.batch.common.job

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

/**
 * 배치 Job 기동 공용 서비스. 스케줄러와 수동 트리거 API 가 공유한다.
 * - businessDate / launchedAt 파라미터 구성 (launchedAt 으로 매 기동 JobInstance 유일화)
 * - 같은 Job 이 실행 중이면 기동을 건너뜀 (중복 기동 방어, 단일 JVM 전제)
 */
@Component
class BatchJobLauncher(
    private val jobLauncher: JobLauncher,
    private val jobExplorer: JobExplorer,
    private val clock: Clock,
    jobs: List<Job>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val jobsByName: Map<String, Job> = jobs.associateBy { it.name }

    val availableJobNames: Set<String>
        get() = jobsByName.keys

    /** businessDate 가 null 이면 운영 타임존 기준 오늘. 이미 실행 중이면 기동을 건너뛰고 null 을 반환한다 */
    fun launch(job: Job, businessDate: LocalDate? = null): JobExecution? {
        val running: Set<JobExecution> = jobExplorer.findRunningJobExecutions(job.name)
        if (running.isNotEmpty()) {
            log.warn("이전 '{}' 실행이 아직 진행 중이라 이번 기동을 건너뜀 (running={})", job.name, running.size)
            return null
        }

        val params: JobParameters = JobParametersBuilder()
            .addString("businessDate", (businessDate ?: LocalDate.now(clock)).toString())
            .addLong("launchedAt", clock.millis())
            .toJobParameters()

        return jobLauncher.run(job, params)
    }

    /** @throws IllegalArgumentException 알 수 없는 Job 이름 */
    fun launchByName(jobName: String, businessDate: LocalDate? = null): JobExecution? {
        val job: Job = jobsByName[jobName]
            ?: throw IllegalArgumentException("알 수 없는 잡 이름: $jobName (가능: $availableJobNames)")

        return launch(job, businessDate)
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :apps:batch:test --tests 'com.neki.batch.common.job.BatchJobLauncherTest'`
Expected: PASS (3 tests)

- [ ] **Step 6: 커밋**

```bash
git add apps/batch
git commit -m "feat/BACKEND-128 배치 잡 런처와 스케줄링 설정 추가"
```

---

### Task 3: sampleJob, 스케줄러, 트리거 API

**Files:**
- Create: `apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobConfig.kt`
- Create: `apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobScheduler.kt`
- Create: `apps/batch/src/main/kotlin/com/neki/batch/common/api/dto/BatchJobResponse.kt`
- Create: `apps/batch/src/main/kotlin/com/neki/batch/common/api/controller/BatchJobController.kt`
- Modify: `apps/batch/src/test/kotlin/com/neki/batch/NekiBatchApplicationTest.kt`

- [ ] **Step 1: 실패하는 테스트 추가** (`NekiBatchApplicationTest` 에 아래 추가)

```kotlin
    @Autowired
    private lateinit var launcher: BatchJobLauncher

    @Test
    fun `sampleJob 을 기동하면 COMPLETED 로 끝난다`() {
        val execution: JobExecution? = launcher.launchByName(SampleJobConfig.JOB_NAME)

        execution.shouldNotBeNull()
        execution.status shouldBe BatchStatus.COMPLETED
    }

    @Test
    fun `트리거 API 로 sampleJob 을 기동할 수 있다`() {
        val response: ResponseEntity<String> =
            restTemplate.postForEntity("/batch/jobs/sampleJob?businessDate=2026-09-23", null, String::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldContain "COMPLETED"
    }

    @Test
    fun `알 수 없는 잡 이름은 404 를 반환한다`() {
        val response: ResponseEntity<String> =
            restTemplate.postForEntity("/batch/jobs/unknownJob", null, String::class.java)

        response.statusCode shouldBe HttpStatus.NOT_FOUND
    }
```

- [ ] **Step 2: SampleJobConfig 작성**

```kotlin
package com.neki.batch.sample

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * 배선 검증용 no-op 잡. 메타 테이블, 런처, 스케줄러, 트리거 API 가 이어지는지 확인하는 용도이며
 * 실제 잡이 들어오면 삭제한다.
 */
@Configuration
class SampleJobConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(JOB_NAME)
    fun sampleJob(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(sampleStep(jobRepository, transactionManager))
            .build()

    private fun sampleStep(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Step =
        StepBuilder("sampleStep", jobRepository)
            .tasklet({ _, chunkContext ->
                val businessDate: Any? = chunkContext.stepContext.jobParameters["businessDate"]
                log.info("sampleJob 실행 (businessDate={})", businessDate)
                RepeatStatus.FINISHED
            }, transactionManager)
            .build()

    companion object {
        const val JOB_NAME = "sampleJob"
    }
}
```

- [ ] **Step 3: SampleJobScheduler 작성**

```kotlin
package com.neki.batch.sample

import com.neki.batch.common.job.BatchJobLauncher
import org.springframework.batch.core.Job
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "neki.batch", name = ["scheduling-enabled"], havingValue = "true")
class SampleJobScheduler(
    private val launcher: BatchJobLauncher,
    @Qualifier(SampleJobConfig.JOB_NAME) private val sampleJob: Job,
) {
    @Scheduled(cron = "\${neki.batch.cron.sample}", zone = "\${neki.batch.zone:Asia/Seoul}")
    fun runSample() {
        launcher.launch(sampleJob)
    }
}
```

- [ ] **Step 4: BatchJobResponse 작성**

```kotlin
package com.neki.batch.common.api.dto

data class BatchJobResponse(
    val jobName: String,
    val executionId: Long?,
    val status: String,
    val exitCode: String,
)
```

- [ ] **Step 5: BatchJobController 작성**

```kotlin
package com.neki.batch.common.api.controller

import com.neki.batch.common.api.dto.BatchJobResponse
import com.neki.batch.common.job.BatchJobLauncher
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import org.springframework.batch.core.JobExecution
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 잡 수동 트리거 API. cron 을 기다리지 않고 잡 기동을 검증한다.
 *
 * 인증이 없으므로 `neki.batch.trigger-api-enabled=true` 인 pod 에서만 빈이 등록되며,
 * k8s Service/Ingress 로 외부에 노출하지 않는다 (kubectl port-forward 로 호출).
 * JobLauncher 가 동기 실행이라 잡이 끝날 때까지 응답이 블로킹된다.
 */
@RestController
@RequestMapping("/batch/jobs")
@ConditionalOnProperty(prefix = "neki.batch", name = ["trigger-api-enabled"], havingValue = "true")
class BatchJobController(private val launcher: BatchJobLauncher) {

    @PostMapping("/{jobName}")
    fun trigger(
        @PathVariable jobName: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) businessDate: LocalDate?,
    ): ResponseEntity<BaseResponse<BatchJobResponse>> {
        if (jobName !in launcher.availableJobNames) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BaseResponse(ResultCode.NOT_FOUND.code, "알 수 없는 잡: $jobName (가능: ${launcher.availableJobNames})"),
            )
        }

        val execution: JobExecution = launcher.launchByName(jobName, businessDate)
            ?: return ResponseEntity.status(HttpStatus.CONFLICT).body(
                BaseResponse(ResultCode.ALREADY_REQUEST.code, "이미 실행 중인 잡: $jobName"),
            )

        return ResponseEntity.ok(
            BaseResponse(
                data = BatchJobResponse(
                    jobName = jobName,
                    executionId = execution.id,
                    status = execution.status.name,
                    exitCode = execution.exitStatus.exitCode,
                ),
            ),
        )
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :apps:batch:test`
Expected: PASS (NekiBatchApplicationTest 4, BatchJobLauncherTest 3)

- [ ] **Step 7: 커밋**

```bash
git add apps/batch
git commit -m "feat/BACKEND-128 sampleJob 과 스케줄러, 수동 트리거 API 추가"
```

---

### Task 4: Flyway V31 메타 테이블

**Files:**
- Create: `modules/postgres/src/main/resources/db/migration/V31__create_spring_batch_meta_tables.sql`

- [ ] **Step 1: 마이그레이션 작성**

Spring Batch 5.2.4 의 `org/springframework/batch/core/schema-postgresql.sql` 을 그대로 쓰되 `IF NOT EXISTS` 를 붙인다. Notification 앱의 `V2__spring_batch_schema.sql` 과 공백 외 동일함을 확인했다.

```sql
-- Spring Batch 5.2.4 메타 테이블 (org/springframework/batch/core/schema-postgresql.sql 미러).
-- apps/batch 의 spring.batch.jdbc.initialize-schema=never 와 정합 — 이 레포의 Flyway 가 메타 스키마를 소유한다.
--
-- IF NOT EXISTS 인 이유: Team-Neki-Notification 앱이 같은 prod DB 에 자기 전용 history 테이블
-- (flyway_schema_history_notification) 로 동일한 테이블을 이미 만들어 두었다. 그 테이블을 이 레포의
-- Flyway 에 편입하기 위해 있으면 그대로 쓰고 없으면(staging, local) 만든다.
-- Spring Batch 업그레이드로 스키마가 바뀌면 후속 마이그레이션으로 반영하고 Notification 쪽도 함께 확인해야 한다.

CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
        REFERENCES BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
        REFERENCES BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
```

- [ ] **Step 2: 로컬 PostgreSQL 로 검증** (Docker 필요)

Run: `docker compose up -d && ./gradlew :apps:batch:bootRun` 후 `psql` 로 `\dt batch_*`, 그리고 두 번째 기동에서 Flyway 가 V31 을 건너뛰는지 확인. Docker 를 못 쓰는 환경이면 api 앱 기동으로 대체 (같은 마이그레이션을 실행한다).

- [ ] **Step 3: 커밋**

```bash
git add modules/postgres
git commit -m "feat/BACKEND-128 Spring Batch 메타 테이블 Flyway V31 추가"
```

---

### Task 5: 배포 배선

**Files:**
- Modify: `Dockerfile`
- Modify: `.github/workflows/deploy-staging.yml`
- Modify: `.github/workflows/deploy-prod.yml`

- [ ] **Step 1: Dockerfile 의 COPY 를 ARG 로 분기**

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS builder

# 빌드할 실행 모듈 (api | batch). 워크플로의 docker build-args 로 넘긴다
ARG APP_MODULE=api

WORKDIR /app

# GitHub Actions에서 빌드된 JAR 파일 복사 (실행 모듈은 :apps:${APP_MODULE})
COPY apps/${APP_MODULE}/build/libs/*.jar app.jar
```

- [ ] **Step 2: 워크플로에 `app` 입력과 분기 추가** (두 파일 동일 패턴)

```yaml
on:
  workflow_dispatch:
    inputs:
      app:
        description: '배포할 앱'
        type: choice
        options:
          - api
          - batch
        default: api

env:
  APP_MODULE: ${{ inputs.app || 'api' }}
  DOCKER_IMAGE: ${{ secrets.DOCKER_USERNAME }}/${{ inputs.app == 'batch' && 'neki-batch-dev' || 'yapp-dev' }}
  GITOPS_REPO: Team-Neki/Team-Neki-GitOps
  GITOPS_PATH: ${{ inputs.app == 'batch' && 'overlays/staging/batch-deployment.yaml' || 'overlays/staging/deployment.yaml' }}
```

prod 는 `neki-batch-prod` / `neki-prod`, `overlays/prod/batch-deployment.yaml` / `overlays/prod/deployment.yaml`. `push: main` 은 `inputs` 가 비어 api 로 동작한다.

빌드, 도커, sed 스텝:

```yaml
      - name: Build with Gradle
        run: ./gradlew :apps:${{ env.APP_MODULE }}:bootJar --no-daemon -x test

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          build-args: |
            APP_MODULE=${{ env.APP_MODULE }}
          ...

      - name: Update image tag in GitOps repo
        run: |
          cd gitops
          NEW_IMAGE="${{ env.DOCKER_IMAGE }}:${{ steps.version.outputs.version }}-${{ steps.version.outputs.short_sha }}"
          IMAGE_REPO="${NEW_IMAGE##*/}"; IMAGE_REPO="${IMAGE_REPO%%:*}"
          sed -i "s|image: .*/${IMAGE_REPO}:.*|image: $NEW_IMAGE|g" ${{ env.GITOPS_PATH }}
```

Discord 알림 title 에 `[${APP_MODULE}]` 를 붙여 어느 앱인지 드러낸다.

- [ ] **Step 3: actionlint 로 검증**

Run: `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest -color`
Expected: 오류 없음

- [ ] **Step 4: 커밋**

```bash
git add Dockerfile .github/workflows
git commit -m "feat/BACKEND-128 Dockerfile 과 배포 워크플로에 batch 앱 분기 추가"
```

---

### Task 6: 문서와 개발 편의 스크립트

**Files:**
- Modify: `Makefile` (`run`, `start` 가 `:apps:api:bootRun` / `:apps:api:bootJar` 만 실행)
- Modify: `README.md` (모듈 설명에 `apps/batch`, 실행 명령을 `:apps:api:bootRun` 으로)
- Modify: `.claude/CLAUDE.md` (Quick Reference 의 bootRun 명령)
- Modify: `.claude/skills/architecture/SKILL.md` (Module Structure 에 `apps/batch/`)
- Modify: `docs/layering-policy.md` (모듈 목록에 `apps/batch/`)

- [ ] **Step 1: 루트 `bootRun` 이 두 앱을 동시에 띄우지 않도록 명령을 한정**

`./gradlew bootRun` 은 bootRun 태스크를 가진 모든 서브프로젝트를 실행하므로 api 와 batch 가 같은 8080 포트에서 충돌한다. Makefile, README, CLAUDE.md 의 명령을 `./gradlew :apps:api:bootRun` 으로 바꾸고 batch 는 `./gradlew :apps:batch:bootRun` 으로 안내한다.

- [ ] **Step 2: 모듈 문서 한 줄씩 추가**

architecture SKILL.md 와 layering-policy.md 의 Module Structure:

```text
apps/batch/    Spring Batch 잡 + 스케줄러 + 수동 트리거 API. 실행 모듈(bootJar). domain 을 재사용하며 스캔 범위는 앱 클래스에 명시
```

- [ ] **Step 3: spotless 와 전체 테스트**

Run: `./gradlew spotlessApply && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add Makefile README.md .claude docs
git commit -m "docs/BACKEND-128 apps/batch 모듈 문서 반영"
```
