package com.neki.photo.api.dto

import com.neki.common.domain.vo.SortOrder
import com.neki.common.properties.AppProperties
import com.neki.photo.application.dto.PhotoImageCommand
import com.neki.photo.application.dto.PhotoImageQuery
import com.neki.photo.application.dto.PhotoImageResult
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoImageConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : PhotoImage api layer converter
 */
object PhotoImageConverter {
    @Component
    class RequestConverter {
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
        fun toUpdatePhotoCommand(userId: Long, photoId: Long, request: UpdatePhotoRequest) =
            PhotoImageCommand.UpdatePhoto(
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

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
        }

        fun toGetPhotosResponse(result: PhotoImageResult.GetPhotos): GetPhotosResponse = GetPhotosResponse(
            totalCount = result.totalCount,
            items = result.photos.map {
                GetPhotosResponse.PhotoInfo(
                    photoId = it.photoId,
                    imageUrl = toImageUrl(it.storageKey),
                    favorite = it.favorite,
                    contentType = it.contentType,
                    width = it.width,
                    height = it.height,
                    memo = it.memo,
                    createdAt = it.createdAt,
                )
            },
            hasNext = result.hasNext,
        )

        fun toGetPhotoResponse(result: PhotoImageResult.GetPhoto): GetPhotoResponse = GetPhotoResponse(
            photoId = result.photoId,
            imageUrl = toImageUrl(result.storageKey),
            favorite = result.favorite,
            contentType = result.contentType,
            width = result.width,
            height = result.height,
            memo = result.memo,
            createdAt = result.createdAt,
        )

        fun toGetFavoriteSummaryResponse(result: PhotoImageResult.GetFavoriteSummary): GetFavoriteSummaryResponse =
            GetFavoriteSummaryResponse(
                latestImageUrl = result.storageKey?.let { toImageUrl(result.storageKey) },
                totalCount = result.totalCount,
            )

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"
    }
}
