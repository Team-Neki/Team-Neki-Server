package com.neki.batch.search.tasklet

import com.neki.batch.search.application.SearchIndexUseCase
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

/**
 * fileName       : SwapSearchTablesTasklet
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : _write 와 _read 의 이름을 맞바꾼다. 락을 짧게만 기다리므로 실패하면 _read 는 그대로이고 다시 돌리면 된다
 */
@Component
class SwapSearchTablesTasklet(private val searchIndexUseCase: SearchIndexUseCase) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        searchIndexUseCase.swap()
        return RepeatStatus.FINISHED
    }
}
