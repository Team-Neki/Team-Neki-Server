package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.application.command.GetMediaStorageInfoCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.result.GetMediaStorageInfoResult

/**
 * fileName       : GetMediaStorageInfoUseCase
 * author         : koo
 * date           : 2026. 1. 26. 오후 7:08
 * description    :
 */
@UseCase
class GetMediaStorageInfoUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(command: GetMediaStorageInfoCommand): GetMediaStorageInfoResult {
        val media = command.ownerId?.let {
            mediaRepository.getActiveMedia(command.ownerId, command.mediaId)
        } ?: mediaRepository.getActiveMedia(command.mediaId)

        if (media == null) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        return GetMediaStorageInfoResult(
            mediaId = media.id!!,
            storageKey = media.storageKey,
            contentType = media.contentType,
            width = media.width,
            height = media.height,
        )
    }
}
