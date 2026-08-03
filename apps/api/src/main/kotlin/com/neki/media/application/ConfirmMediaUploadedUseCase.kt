package com.neki.media.application

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaCommand
import com.neki.media.models.UploadConfirmStatus
import com.neki.media.service.MediaService

/**
 * fileName       : VerifyMediaUseCase
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:47
 * description    : Object Storage에 정상적으로 저장됐는지 확인하는 usecase
 */
@UseCase
class ConfirmMediaUploadedUseCase(
    private val mediaService: MediaService,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: MediaCommand.ConfirmMediasUploaded): MediaResult.ConfirmMediasUploaded {
        if (command.mediaIds.isEmpty()) return MediaResult.ConfirmMediasUploaded(emptyMap())

        val storageExistsMap: Map<Long, Boolean> = mediaService.getExistsMap(command)

        val statuses: Map<Long, UploadConfirmStatus> =
            transactionRunner.runNew { mediaService.confirmMediasUploaded(command, storageExistsMap) }

        return MediaResult.ConfirmMediasUploaded(statuses)
    }

    /**
     * 보상 트랜잭션: media 상태를 INITIATED로 롤백
     * PhotoImage 저장 실패 시 호출
     */
    fun rollback(command: MediaCommand.ConfirmMediasUploaded) {
        if (command.mediaIds.isEmpty()) return

        transactionRunner.runNew { mediaService.rollbackToInitiated(command) }
    }
}
