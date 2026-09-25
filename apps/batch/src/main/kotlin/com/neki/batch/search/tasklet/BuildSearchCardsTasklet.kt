package com.neki.batch.search.tasklet

import com.neki.batch.search.application.SearchIndexUseCase
import com.neki.batch.search.application.dto.SearchIndexResult
import com.neki.batch.search.job.SearchIndexJobConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * fileName       : BuildSearchCardsTasklet
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 잡 파라미터 businessDate 를 읽어 SearchIndexUseCase.build 를 한 번 부른다 (_write 채움).
 *                  예외는 그대로 올려 step 을 FAILED 로 만든다
 */
@Component
class BuildSearchCardsTasklet(private val searchIndexUseCase: SearchIndexUseCase) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val raw: Any =
            requireNotNull(chunkContext.stepContext.jobParameters[SearchIndexJobConfig.PARAM_BUSINESS_DATE]) {
                "${SearchIndexJobConfig.PARAM_BUSINESS_DATE} 파라미터가 없습니다 (예: ${SearchIndexJobConfig.PARAM_BUSINESS_DATE}=2026-09-25)"
            }
        val businessDate: LocalDate = LocalDate.parse(raw.toString())
        val result: SearchIndexResult = searchIndexUseCase.build(businessDate)
        log.info("{} 완료 (businessDate={}, result={})", SearchIndexStepConfig.BUILD_STEP_NAME, businessDate, result)
        return RepeatStatus.FINISHED
    }
}
