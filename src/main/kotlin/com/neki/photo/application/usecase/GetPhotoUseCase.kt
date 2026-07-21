package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.contract.MediaStorageInfo
import com.neki.photo.application.dto.PhotoImageQuery
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort

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

    fun execute(query: PhotoImageQuery.GetPhoto): PhotoImageResult.GetPhoto {
        val (photo, isFavorite) = photoRepository.getOwnedPhotoWithFavorite(query.userId, query.photoId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val mediaInfo: MediaStorageInfo = mediaClient.getMediaStorageInfo(query.userId, photo.mediaId)

        return PhotoImageResult.GetPhoto(
            photoId = photo.id!!,
            storageKey = mediaInfo.storageKey,
            favorite = isFavorite,
            contentType = mediaInfo.contentType,
            uploadMethod = photo.uploadMethod,
            width = mediaInfo.width,
            height = mediaInfo.height,
            memo = photo.memo,
            createdAt = photo.createdAt!!,
            capturedAt = photo.capturedAt,
        )
    }
}
