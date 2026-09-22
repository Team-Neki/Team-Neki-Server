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
