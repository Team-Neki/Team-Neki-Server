package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.ConfirmMediaUploadedCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.ConfirmMediaUploadedResult

/**
 * fileName       : VerifyMediaUseCase
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:47
 * description    : Object Storage에 정상적으로 저장됐는지 확인하는 usecase
 */
@UseCase
class ConfirmMediaUploadedUseCase(
    private val mediaRepository: MediaRepositoryPort,
    private val mediaStorage: MediaStoragePort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: ConfirmMediaUploadedCommand): ConfirmMediaUploadedResult {
        val media = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 이미 업로드된 경우 무시
        if (media.isUploaded()) {
            return ConfirmMediaUploadedResult(true)
        }

        // 오브젝트 스토리지에 해당 키가 존재하는지 확인
        val exists = mediaStorage.exists(media.storageKey)

        return transactionRunner.runNew {
            val media = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaId)
                ?: throw BusinessException(ResultCode.NOT_FOUND)

            if (media.isUploaded() || exists) {
                media.markAsUploaded()
                ConfirmMediaUploadedResult(true)
            } else {
                media.markAsFailed()
                ConfirmMediaUploadedResult(false)
            }
        }
    }
}
