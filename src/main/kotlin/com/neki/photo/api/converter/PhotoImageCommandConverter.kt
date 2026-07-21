package com.neki.photo.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.api.dto.DeletePhotosRequest
import com.neki.photo.api.dto.UpdatePhotoRequest
import com.neki.photo.api.dto.UploadPhotoRequest
import com.neki.photo.application.dto.PhotoImageCommand
import com.neki.photo.application.dto.PhotoImageQuery
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoImageCommandConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:30
 * description    : Photo image application layer command 변경을 위한 converter
 */
@Component
class PhotoImageCommandConverter {

    fun toUploadPhotoCommand(userId: Long, request: UploadPhotoRequest) = PhotoImageCommand.UploadPhoto(
        userId = userId,
        folderId = request.folderId,
        uploads = request.uploads.map { item ->
            PhotoImageCommand.UploadPhoto.UploadItem(
                mediaId = item.mediaId!!,
                uploadMethod = item.uploadMethod,
                memo = item.memo,
                capturedAt = item.capturedAt,
            )
        },
        favorite = request.favorite ?: false,
    )

    fun toGetPhotosQuery(
        userId: Long,
        folderId: Long?,
        page: Int,
        size: Int,
        sortOrder: SortOrder,
    ): PhotoImageQuery.GetPhotos = PhotoImageQuery.GetPhotos(
        userId = userId,
        folderId = folderId,
        page = page,
        size = size,
        sortOrder = sortOrder,
    )

    fun toGetPhotoQuery(userId: Long, photoId: Long): PhotoImageQuery.GetPhoto = PhotoImageQuery.GetPhoto(
        userId = userId,
        photoId = photoId,
    )

    fun toDeletePhotosCommand(userId: Long, request: DeletePhotosRequest) = PhotoImageCommand.DeletePhotos(
        userId = userId,
        photoIds = request.photoIds,
    )

    @Deprecated(message = "PUT API 변경 후 제거")
    fun toUpdatePhotoCommand(userId: Long, photoId: Long, request: UpdatePhotoRequest) = PhotoImageCommand.UpdatePhoto(
        userId = userId,
        photoId = photoId,
        memo = request.memo,
    )

    fun toPutPhotoCommand(userId: Long, photoId: Long, request: UpdatePhotoRequest) = PhotoImageCommand.PutPhoto(
        userId = userId,
        photoId = photoId,
        memo = request.memo,
        capturedAt = request.capturedAt,
    )
}
