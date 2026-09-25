package com.neki.batch.search.tasklet

import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

/**
 * fileName       : SwapSearchTablesTasklet
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : _write 와 _read 의 이름을 맞바꾼다. TaskletStep 의 트랜잭션 안에서 돌아 밖에서는 _tmp 가 보이지 않는다.
 *                  락을 짧게만 기다리므로 실패하면 _read 는 그대로이고 잡을 다시 돌리면 된다
 */
@Component
class SwapSearchTablesTasklet(private val repository: PhotoBoothSearchRepository) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        repository.swap()
        log.info("{} 완료 (_write -> _read)", SearchIndexStepConfig.SWAP_STEP_NAME)
        return RepeatStatus.FINISHED
    }
}
