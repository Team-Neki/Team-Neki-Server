package com.neki.batch.sample

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * 배선 검증용 no-op 잡. 메타 테이블, 기동 시 실행, 종료 코드가 이어지는지 확인하는 용도이며
 * 실제 잡이 들어오면 삭제한다.
 *
 * RunIdIncrementer: 같은 파라미터(businessDate)로 다시 기동해도 항상 새 JobInstance 로 처음부터 돈다.
 * 없으면 두 번째 기동이 JobInstanceAlreadyCompleteException 으로 죽는다.
 * 직전 실행이 FAILED 여도 같은 인스턴스를 재시작(restart)하지 않는다. Spring Batch 5.2 의
 * JobParametersBuilder.getNextJobParameters 는 무조건 run.id 를 올리고, Boot 의 restart 분기는
 * 커맨드라인 파라미터만으로 만든 키(run.id 없음)로 인스턴스를 찾으므로 항상 빗나간다.
 * 따라서 이 앱의 잡은 재시도 시 처음부터 다시 돌아도 되게 멱등해야 한다.
 * (incrementer 가 없는 잡은 반대로 FAILED 인스턴스를 재시작하고, COMPLETED 인 파라미터로는 죽는다.)
 */
@Configuration
class SampleJobConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(JOB_NAME)
    fun sampleJob(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
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
