package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.media.application.dto.MediaCommand
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.entity.Media
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : DeleteMediaUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:13
 * description    : media delete usecase
 * - media delete usecase는 media 단건 삭제를 위한 usecase로 현재 다른 도메인에서 명시적으로 호출되고 있지 않습니다.
 * 추후 이미지 단건 삭제가 필요한 경우를 고려하여 해당 usecase를 남겨두었습니다.
 */
@UseCase
class DeleteMediaUseCase(
    private val mediaRepository: MediaRepositoryPort,
    private val cache: MediaBinaryCachePort,
    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * media 단건 삭제 usecase
     */
    fun execute(command: MediaCommand.DeleteMedia) {
        val media: Media = transactionRunner.run {
            val foundMedia: Media = mediaRepository.getActiveMedia(command.ownerId, command.mediaId)
                ?: throw BusinessException(ResultCode.NOT_FOUND)

            foundMedia.markAsDeleted()
            mediaRepository.save(foundMedia)
            foundMedia
        }

        cache.evict(media.storageKey)
    }

    /**
     * media bulk 삭제 usecase
     */
    fun execute(command: MediaCommand.DeleteMedias) {
        val medias: List<Media> = transactionRunner.run {
            val foundMedias: List<Media> = mediaRepository.getActiveMedias(command.ownerId, command.mediaIds)
            foundMedias.forEach { it.markAsDeleted() }
            mediaRepository.saveAll(foundMedias)
            foundMedias
        }

        medias.forEach { cache.evict(it.storageKey) }
    }
}
