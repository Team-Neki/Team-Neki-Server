package com.neki.batch.search.tasklet

import org.springframework.batch.core.Step
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * fileName       : SearchIndexStepConfig
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 재생성 step. tasklet 하나가 전부이며 step 의 트랜잭션 안에서 돈다
 */
@Configuration
class SearchIndexStepConfig {

    @Bean(STEP_NAME)
    fun searchIndexStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        searchIndexTasklet: SearchIndexTasklet,
    ): Step = StepBuilder(STEP_NAME, jobRepository)
        .tasklet(searchIndexTasklet, transactionManager)
        .build()

    companion object {
        const val STEP_NAME = "searchIndexStep"
    }
}
