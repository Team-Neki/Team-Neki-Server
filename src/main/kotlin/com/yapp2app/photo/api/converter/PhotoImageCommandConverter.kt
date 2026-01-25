package com.yapp2app.photo.api.converter

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.photo.api.dto.DeletePhotosRequest
import com.yapp2app.photo.api.dto.UpdatePhotoRequest
import com.yapp2app.photo.api.dto.UploadPhotoRequest
import com.yapp2app.photo.application.command.DeletePhotosCommand
import com.yapp2app.photo.application.command.GetPhotosCommand
import com.yapp2app.photo.application.command.UpdatePhotoCommand
import com.yapp2app.photo.application.command.UploadPhotoCommand
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoImageCommandConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:30
 * description    : Photo image application layer command 변경을 위한 converter
 */
@Component
class PhotoImageCommandConverter {

    fun toUploadPhotoCommand(userId: Long, request: UploadPhotoRequest) = UploadPhotoCommand(
        userId = userId,
        folderId = request.folderId,
        uploads = request.uploads.map { item ->
            UploadPhotoCommand.UploadItem(
                mediaId = item.mediaId!!,
                memo = item.memo,
            )
        },
    )

    fun toGetPhotosCommand(
        userId: Long,
        folderId: Long?,
        page: Int,
        size: Int,
        sortOrder: SortOrder,
    ): GetPhotosCommand = GetPhotosCommand(
        userId = userId,
        folderId = folderId,
        page = page,
        size = size,
        sortOrder = sortOrder,
    )

    fun toDeletePhotosCommand(userId: Long, request: DeletePhotosRequest) = DeletePhotosCommand(
        userId = userId,
        photoIds = request.photoIds,
    )

    fun toUpdatePhotoCommand(userId: Long, photoId: Long, request: UpdatePhotoRequest) = UpdatePhotoCommand(
        userId = userId,
        photoId = photoId,
        memo = request.memo,
    )
}
