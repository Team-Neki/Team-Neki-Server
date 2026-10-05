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
 * description    : 검색 카드 재생성 step 둘. 각 step 은 tasklet 하나이고 자기 트랜잭션 안에서 돈다
 */
@Configuration
class SearchIndexStepConfig {

    @Bean(BUILD_STEP_NAME)
    fun buildSearchCardsStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        buildSearchCardsTasklet: BuildSearchCardsTasklet,
    ): Step = StepBuilder(BUILD_STEP_NAME, jobRepository)
        .tasklet(buildSearchCardsTasklet, transactionManager)
        .build()

    @Bean(SWAP_STEP_NAME)
    fun swapSearchTablesStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        swapSearchTablesTasklet: SwapSearchTablesTasklet,
    ): Step = StepBuilder(SWAP_STEP_NAME, jobRepository)
        .tasklet(swapSearchTablesTasklet, transactionManager)
        .build()

    companion object {
        const val BUILD_STEP_NAME = "buildSearchCardsStep"
        const val SWAP_STEP_NAME = "swapSearchTablesStep"
    }
}
