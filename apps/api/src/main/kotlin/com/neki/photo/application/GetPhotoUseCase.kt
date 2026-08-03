package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.photo.MediaClient
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.dto.PhotoImageQuery
import com.neki.photo.models.MediaMetadata
import com.neki.photo.service.PhotoService

/**
 * fileName       : GetPhotoUseCase
 * author         : koo
 * date           : 2026. 1. 25. 오전 12:53
 * description    :
 */
@UseCase
class GetPhotoUseCase(private val photoService: PhotoService, private val mediaClient: MediaClient) {

    fun execute(query: PhotoImageQuery.GetPhoto): PhotoImageResult.GetPhoto {
        val (photo, isFavorite) = photoService.getOwnedPhotoWithFavorite(query)

        val mediaInfo: MediaMetadata = mediaClient.getMediaMetadata(query.userId, photo.mediaId)

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
