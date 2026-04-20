package com.neki.photo.api.converter

import com.neki.common.properties.AppProperties
import com.neki.photo.api.dto.GetFavoriteSummaryResponse
import com.neki.photo.api.dto.GetPhotoResponse
import com.neki.photo.api.dto.GetPhotosResponse
import com.neki.photo.application.result.GetFavoriteSummaryResult
import com.neki.photo.application.result.GetPhotoResult
import com.neki.photo.application.result.GetPhotosResult
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

    fun toGetPhotosResponse(result: GetPhotosResult): GetPhotosResponse = GetPhotosResponse(
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

    fun toGetPhotoResponse(result: GetPhotoResult): GetPhotoResponse = GetPhotoResponse(
        photoId = result.photoId,
        imageUrl = toImageUrl(result.storageKey),
        favorite = result.favorite,
        contentType = result.contentType,
        width = result.width,
        height = result.height,
        memo = result.memo,
        createdAt = result.createdAt,
    )

    fun toGetFavoriteSummaryResponse(result: GetFavoriteSummaryResult): GetFavoriteSummaryResponse =
        GetFavoriteSummaryResponse(
            latestImageUrl = result.storageKey?.let { toImageUrl(result.storageKey) },
            totalCount = result.totalCount,
        )

    private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"
}
