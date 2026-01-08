package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.DeleteMediaCommand
import com.yapp2app.media.application.command.DeleteMediasCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import org.slf4j.LoggerFactory

/**
 * fileName       : DeleteMediaUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:13
 * description    : media delete usecase
 * -
 */
@UseCase
class DeleteMediaUseCase(
    private val mediaRepository: MediaRepositoryPort,
    private val mediaStorage: MediaStoragePort,
    private val transactionRunner: TransactionRunner,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * media 단건 삭제 usecase
     */
    fun execute(command: DeleteMediaCommand) {
        // DB media metadata 삭제
        val media = transactionRunner.run {
            val foundMedia = mediaRepository.getActiveMedia(command.ownerId, command.mediaId)
                ?: throw BusinessException(ResultCode.NOT_FOUND)

            foundMedia.markAsDeleteRequested()
            foundMedia
        }

        return runCatching {
            // object storage 삭제 요청
            mediaStorage.deleteByKey(media.storageKey)
        }.fold(
            onSuccess = {
                transactionRunner.runNew {
                    // 추후 scheduling 검토
                    mediaRepository.delete(media.id!!)
                }
            },
            onFailure = { e ->
                log.warn(
                    "Media delete failed. Will retry later. fileId={}, key={}",
                    media.id,
                    media.storageKey,
                    e,
                )
            },
        )

        // TODO : object storage 삭제 실패한 media에 대해 삭제 필요
    }

    /**
     * media bulk 삭제 usecase
     */
    fun execute(command: DeleteMediasCommand) {
        // 삭제할 media 조회
        val medias = transactionRunner.run {
            val foundMedias =
                mediaRepository.getActiveMedias(command.ownerId, command.mediaIds)

            foundMedias.forEach { it.markAsDeleteRequested() }
            foundMedias
        }

        val deletedMediaIds = mutableListOf<Long>()
        val failedMedias = mutableListOf<Long>()

        // object storage 삭제
        medias.forEach { media ->
            runCatching {
                mediaStorage.deleteByKey(media.storageKey)
            }.onSuccess {
                deletedMediaIds.add(media.id!!)
            }.onFailure { e ->
                failedMedias.add(media.id!!)
                log.warn(
                    "Media delete failed. Will retry later. mediaId={}, key={}",
                    media.id,
                    media.storageKey,
                    e,
                )
            }
        }

        // object storage 삭제 성공한 오브젝트에 대해 DB soft delete
        if (deletedMediaIds.isNotEmpty()) {
            transactionRunner.runNew {
                mediaRepository.deleteAll(deletedMediaIds)
            }
        }

        // TODO : object storage 삭제 실패한 media에 대해 삭제 필요
    }
}
