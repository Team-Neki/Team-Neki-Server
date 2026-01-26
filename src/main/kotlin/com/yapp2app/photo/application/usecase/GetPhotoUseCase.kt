package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.GetPhotoCommand
import com.yapp2app.photo.application.contract.MediaStorageInfo
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.application.result.GetPhotoResult

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
            createdAt = photo.createdAt!!,
        )
    }
}
