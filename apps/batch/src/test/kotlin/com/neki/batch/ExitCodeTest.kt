package com.neki.batch

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager

/**
 * Prefect 가 의존하는 계약: 기동 시 Job 을 실행하고 그 상태가 프로세스 종료 코드가 된다.
 * main 의 exitProcess(SpringApplication.exit(context)) 와 같은 경로를 밟되 JVM 은 끝내지 않는다.
 * 다른 테스트의 캐시된 컨텍스트와 H2 를 공유하지 않도록 테스트마다 별도 DB 이름을 쓴다.
 */
class ExitCodeTest {

    @Test
    fun `완료된 Job 은 종료 코드 0`() {
        val context: ConfigurableApplicationContext = boot("sampleJob", db = "ok")

        SpringApplication.exit(context) shouldBe 0
    }

    @Test
    fun `실패한 Job 은 0 이 아닌 종료 코드`() {
        val context: ConfigurableApplicationContext = boot(FailingJobConfig.JOB_NAME, db = "fail")

        SpringApplication.exit(context) shouldNotBe 0
    }

    @Test
    fun `같은 businessDate 로 두 번 기동하면 둘 다 성공하고 새 JobInstance 로 돈다`() {
        SpringApplication.exit(boot("sampleJob", db = "rerun")) shouldBe 0

        val second: ConfigurableApplicationContext = boot("sampleJob", db = "rerun")
        val instances: Long = second.getBean(JobExplorer::class.java).getJobInstanceCount("sampleJob")
        SpringApplication.exit(second) shouldBe 0

        instances shouldBe 2
    }

    @Test
    fun `실패한 뒤 같은 인자로 다시 기동해도 재시작이 아니라 새 JobInstance 로 돈다`() {
        SpringApplication.exit(boot(FailingJobConfig.JOB_NAME, db = "retry")) shouldNotBe 0

        val second: ConfigurableApplicationContext = boot(FailingJobConfig.JOB_NAME, db = "retry")
        val instances: Long =
            second.getBean(JobExplorer::class.java).getJobInstanceCount(FailingJobConfig.JOB_NAME)
        SpringApplication.exit(second) shouldNotBe 0

        instances shouldBe 2
    }

    @Test
    fun `없는 잡 이름이면 기동 자체가 실패한다`() {
        val error: Throwable = shouldThrowAny { boot("unknownJob", db = "unknown") }

        error.stackTraceToString() shouldContain "No job found with name 'unknownJob'"
    }

    // 운영과 같이 커맨드라인 인자로 넘긴다. properties() 는 기본값이라 application-test.yml 에 덮이고,
    // profiles() 는 application.yaml 의 local 을 대체하지 않고 더하므로 둘 다 쓰지 않는다.
    // --옵션은 JobParameters 에서 빠지고 businessDate=… 만 들어간다
    private fun boot(jobName: String, db: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(NekiBatchApplication::class.java, FailingJobConfig::class.java)
            .run(
                "--spring.profiles.active=test",
                "--spring.batch.job.enabled=true",
                "--spring.batch.job.name=$jobName",
                "--spring.datasource.url=jdbc:h2:mem:exitcode-$db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "businessDate=2026-09-23",
            )

    /**
     * 실제 잡과 같이 RunIdIncrementer 를 둔다. incrementer 가 없는 잡은 반대로 FAILED 인스턴스를 같은 인자로
     * 재시작(restart)하므로, 이 설정이 빠지면 "새 JobInstance" 테스트가 의미를 잃는다.
     */
    @TestConfiguration
    class FailingJobConfig {

        @Bean(JOB_NAME)
        fun failingJob(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Job =
            JobBuilder(JOB_NAME, jobRepository)
                .incrementer(RunIdIncrementer())
                .start(
                    StepBuilder("failingStep", jobRepository)
                        .tasklet({ _, _ -> throw IllegalStateException("의도된 실패") }, transactionManager)
                        .build(),
                )
                .build()

        companion object {
            const val JOB_NAME = "failingJob"
        }
    }
}
