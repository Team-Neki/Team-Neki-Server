package com.neki.batch.search.job

import com.neki.batch.search.tasklet.SearchIndexStepConfig
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : SearchIndexJobConfig
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 재생성 잡. build(_write 채움) -> swap(_read 와 맞바꿈) 두 step 이다.
 *
 * RunIdIncrementer: 같은 파라미터(businessDate)로 다시 기동해도 항상 새 JobInstance 로 처음부터 돈다.
 * 없으면 두 번째 기동이 JobInstanceAlreadyCompleteException 으로 죽는다.
 * 직전 실행이 FAILED 여도 같은 인스턴스를 재시작(restart)하지 않으므로 (Spring Batch 5.2 의
 * getNextJobParameters 가 무조건 run.id 를 올림) 잡은 처음부터 다시 돌아도 되게 멱등해야 한다.
 * build 는 _write 만 갈아엎고 swap 은 이름만 바꾸므로 어느 step 에서 죽어도 _read 는 그대로다.
 * 예외는 tasklet 밖으로 그대로 던져 step FAILED -> job FAILED -> 종료 코드 0 이 아님 (BACKEND-128 계약).
 * 실패 알림은 그 종료 코드를 받은 Prefect Automation 이 보낸다 (BACKEND-142).
 */
@Configuration
class SearchIndexJobConfig {

    @Bean(JOB_NAME)
    fun searchIndexJob(
        jobRepository: JobRepository,
        @Qualifier(SearchIndexStepConfig.BUILD_STEP_NAME) buildSearchCardsStep: Step,
        @Qualifier(SearchIndexStepConfig.SWAP_STEP_NAME) swapSearchTablesStep: Step,
    ): Job = JobBuilder(JOB_NAME, jobRepository)
        .incrementer(RunIdIncrementer())
        .start(buildSearchCardsStep)
        .next(swapSearchTablesStep)
        .build()

    companion object {
        const val JOB_NAME = "searchIndexJob"

        /** Prefect 가 넘기는 잡 파라미터. 어느 수집 사이클의 카드인지 (businessDate=2026-09-25) */
        const val PARAM_BUSINESS_DATE = "businessDate"
    }
}
