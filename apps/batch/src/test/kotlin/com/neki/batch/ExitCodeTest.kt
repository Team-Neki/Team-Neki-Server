package com.neki.batch

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.batch.core.job.builder.JobBuilder
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
 * 다른 테스트의 캐시된 컨텍스트와 H2 를 공유하지 않도록 별도 DB 이름을 쓴다.
 */
class ExitCodeTest {

    @Test
    fun `완료된 Job 은 종료 코드 0`() {
        val context: ConfigurableApplicationContext = boot("sampleJob")

        SpringApplication.exit(context) shouldBe 0
    }

    @Test
    fun `실패한 Job 은 0 이 아닌 종료 코드`() {
        val context: ConfigurableApplicationContext = boot(FailingJobConfig.JOB_NAME)

        SpringApplication.exit(context) shouldNotBe 0
    }

    // properties() 는 기본값이라 application-test.yml 의 job.enabled=false 에 덮인다.
    // 운영과 같이 커맨드라인 인자로 넘긴다. --옵션은 JobParameters 에서 빠지고 businessDate=… 만 들어간다
    private fun boot(jobName: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(NekiBatchApplication::class.java, FailingJobConfig::class.java)
            .profiles("test")
            .run(
                "--spring.batch.job.enabled=true",
                "--spring.batch.job.name=$jobName",
                "--spring.datasource.url=jdbc:h2:mem:exitcode-$jobName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "businessDate=2026-09-23",
            )

    @TestConfiguration
    class FailingJobConfig {

        @Bean(JOB_NAME)
        fun failingJob(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Job =
            JobBuilder(JOB_NAME, jobRepository)
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
