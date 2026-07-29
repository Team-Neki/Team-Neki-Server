package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.application.dto.MediaQuery
import com.neki.media.application.dto.MediaResult
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.domain.entity.Media

/**
 * fileName       : GetMediaStorageInfoUseCase
 * author         : koo
 * date           : 2026. 1. 26. 오후 7:08
 * description    :
 */
@UseCase
class GetMediaStorageInfoUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(query: MediaQuery.GetMediaStorageInfo): MediaResult.GetMediaStorageInfo {
        val media: Media? = query.ownerId?.let {
            mediaRepository.getActiveMedia(query.ownerId, query.mediaId)
        } ?: mediaRepository.getActiveMedia(query.mediaId)

        if (media == null) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        return MediaResult.GetMediaStorageInfo(
            mediaId = media.id!!,
            storageKey = media.storageKey,
            contentType = media.contentType,
            width = media.width,
            height = media.height,
        )
    }
}
