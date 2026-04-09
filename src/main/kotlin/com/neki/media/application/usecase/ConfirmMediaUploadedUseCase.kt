package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.media.application.command.ConfirmMediasUploadedCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.ConfirmMediasUploadedResult
import com.neki.media.application.result.ConfirmMediasUploadedResult.UploadConfirmStatus
import com.neki.media.domain.entity.Media

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

        val medias: List<Media> = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)

        val s3ExistsMap: Map<Long, Boolean> = medias
            .filter { !it.isUploaded() }
            .associate { it.id!! to mediaStorage.exists(it.storageKey) }

        return transactionRunner.runNew {
            val freshMedias: List<Media> = mediaRepository.getMediaForUploadConfirmation(
                command.ownerId,
                command.mediaIds,
            )

            val freshMediaMap: Map<Long, Media> = freshMedias.associateBy { it.id!! }

            val results: Map<Long, UploadConfirmStatus> = command.mediaIds.associateWith { mediaId ->
                val media = freshMediaMap[mediaId]
                when {
                    media == null -> UploadConfirmStatus.NOT_FOUND
                    media.isUploaded() -> UploadConfirmStatus.CONFIRMED
                    s3ExistsMap[mediaId] == true -> {
                        media.markAsUploaded()
                        mediaRepository.save(media)
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
            val medias: List<Media> = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)
            medias.forEach {
                it.markAsInitiated()
                mediaRepository.save(it)
            }
        }
    }
}
