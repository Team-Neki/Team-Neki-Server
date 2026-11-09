package com.neki.api.media.application

import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.service.MediaDirectUploadService

/**
 * fileName       : DiscardMediaUseCase
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 직접 올린 media 를 되돌린다 (스토리지 삭제 + DELETED). 일괄 업로드의 보상 경로에서 쓴다.
 */
@UseCase
class DiscardMediaUseCase(
    private val mediaDirectUploadService: MediaDirectUploadService,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: MediaCommand.DiscardMedia) {
        transactionRunner.run { mediaDirectUploadService.discard(command) }
    }
}
