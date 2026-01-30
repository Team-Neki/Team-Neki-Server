package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.ConfirmMediasUploadedCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.ConfirmMediasUploadedResult
import com.yapp2app.media.application.result.ConfirmMediasUploadedResult.UploadConfirmStatus

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

    fun execute(command: ConfirmMediasUploadedCommand): ConfirmMediasUploadedResult {
        if (command.mediaIds.isEmpty()) return ConfirmMediasUploadedResult(emptyMap())

        val medias = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)

        val s3ExistsMap = medias
            .filter { !it.isUploaded() }
            .associate { it.id!! to mediaStorage.exists(it.storageKey) }

        return transactionRunner.runNew {
            val freshMedias = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)

            val freshMediaMap = freshMedias.associateBy { it.id!! }

            val results = command.mediaIds.associateWith { mediaId ->
                val media = freshMediaMap[mediaId]
                when {
                    media == null -> UploadConfirmStatus.NOT_FOUND
                    media.isUploaded() -> UploadConfirmStatus.CONFIRMED
                    s3ExistsMap[mediaId] == true -> {
                        media.markAsUploaded()
                        UploadConfirmStatus.CONFIRMED
                    }
                    else -> UploadConfirmStatus.NOT_UPLOADED
                }
            }
            ConfirmMediasUploadedResult(results)
        }
    }

    /**
     * 보상 트랜잭션: media 상태를 INITIATED로 롤백
     * PhotoImage 저장 실패 시 호출
     */
    fun rollback(command: ConfirmMediasUploadedCommand) {
        if (command.mediaIds.isEmpty()) return

        transactionRunner.runNew {
            val medias = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)
            medias.forEach { it.markAsInitiated() }
        }
    }
}
