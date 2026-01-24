package com.yapp2app.photo.api.converter

import com.yapp2app.common.properties.AppProperties
import com.yapp2app.photo.api.dto.GetFavoriteSummaryResponse
import com.yapp2app.photo.api.dto.GetPhotosResponse
import com.yapp2app.photo.api.dto.UploadPhotoResponse
import com.yapp2app.photo.application.result.GetFavoriteSummaryResult
import com.yapp2app.photo.application.result.GetPhotosResult
import com.yapp2app.photo.application.result.UploadPhotoResult
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoImageResponseConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:30
 * description    : Photo image api layer response 변경을 위한 converter
 */
@Component
class PhotoImageResultConverter(private val appProperties: AppProperties) {
    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toUploadPhotoResponse(result: UploadPhotoResult): UploadPhotoResponse = UploadPhotoResponse(result.photoId)

    fun toGetPhotosResponse(result: GetPhotosResult): GetPhotosResponse = GetPhotosResponse(
        items = result.photos.map {
            GetPhotosResponse.PhotoInfo(
                photoId = it.photoId,
                imageUrl = toImageUrl(it.storageKey),
                folderId = it.folderId,
                favorite = it.favorite,
                contentType = it.contentType,
                createdAt = it.createdAt,
            )
        },
        hasNext = result.hasNext,
    )

    fun toGetFavoriteSummaryResponse(result: GetFavoriteSummaryResult): GetFavoriteSummaryResponse =
        GetFavoriteSummaryResponse(
            latestImageUrl = result.latestImageUrl,
            totalCount = result.totalCount,
        )

    private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"
}
