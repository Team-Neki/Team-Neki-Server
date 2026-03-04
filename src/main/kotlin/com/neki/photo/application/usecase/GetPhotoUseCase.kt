package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.GetPhotoCommand
import com.neki.photo.contract.MediaStorageInfo
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.application.result.GetPhotoResult

/**
 * fileName       : GetPhotoUseCase
 * author         : koo
 * date           : 2026. 1. 25. 오전 12:53
 * description    :
 */
@UseCase
class GetPhotoUseCase(
    private val photoRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,
) {

    fun execute(command: GetPhotoCommand): GetPhotoResult {
        val (photo, isFavorite) = photoRepository.getOwnedPhotoWithFavorite(command.userId, command.photoId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val mediaInfo: MediaStorageInfo = mediaClient.getMediaStorageInfo(command.userId, photo.mediaId)

        return GetPhotoResult(
            photoId = photo.id!!,
            storageKey = mediaInfo.storageKey,
            favorite = isFavorite,
            contentType = mediaInfo.contentType,
            width = mediaInfo.width,
            height = mediaInfo.height,
            createdAt = photo.createdAt!!,
        )
    }
}
