package com.neki.batch.search

import com.neki.domain.search.service.SearchIndexResult
import com.neki.domain.search.service.SearchIndexService
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
import java.time.LocalDate

/**
 * fileName       : SearchIndexJobConfig
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 전량 재생성 잡. tasklet 하나가 SearchIndexService.rebuild 를 부른다.
 *
 * RunIdIncrementer: 같은 파라미터(businessDate)로 다시 기동해도 항상 새 JobInstance 로 처음부터 돈다.
 * 없으면 두 번째 기동이 JobInstanceAlreadyCompleteException 으로 죽는다.
 * 직전 실행이 FAILED 여도 같은 인스턴스를 재시작(restart)하지 않으므로 (Spring Batch 5.2 의
 * getNextJobParameters 가 무조건 run.id 를 올림) 잡은 처음부터 다시 돌아도 되게 멱등해야 한다.
 * rebuild 는 DELETE + INSERT 를 한 트랜잭션에서 하므로 그 조건을 만족한다.
 * 예외는 tasklet 밖으로 그대로 던져 step FAILED -> job FAILED -> 종료 코드 0 이 아님 (BACKEND-128 계약).
 */
@Configuration
class SearchIndexJobConfig(private val searchIndexService: SearchIndexService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(JOB_NAME)
    fun searchIndexJob(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(searchIndexStep(jobRepository, transactionManager))
            .build()

    private fun searchIndexStep(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Step =
        StepBuilder("searchIndexStep", jobRepository)
            .tasklet({ _, chunkContext ->
                val raw: Any = requireNotNull(chunkContext.stepContext.jobParameters["businessDate"]) {
                    "businessDate 파라미터가 없습니다 (예: businessDate=2026-09-25)"
                }
                val businessDate: LocalDate = LocalDate.parse(raw.toString())
                val result: SearchIndexResult = searchIndexService.rebuild(businessDate)
                log.info("searchIndexJob 완료 (businessDate={}, result={})", businessDate, result)
                RepeatStatus.FINISHED
            }, transactionManager)
            .build()

    companion object {
        const val JOB_NAME = "searchIndexJob"
    }
}
