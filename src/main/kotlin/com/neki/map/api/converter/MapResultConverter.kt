package com.neki.map.api.converter

import com.neki.common.properties.AppProperties
import com.neki.map.api.dto.CollectPhotoBoothResponse
import com.neki.map.api.dto.GetBrandResponse
import com.neki.map.api.dto.GetFavoriteMapResponse
import com.neki.map.api.dto.GetPointLocationResponse
import com.neki.map.api.dto.GetPolygonLocationResponse
import com.neki.map.application.result.CollectPhotoBoothResult
import com.neki.map.application.result.GetBrandResult
import com.neki.map.application.result.GetFavoriteMapResult
import com.neki.map.application.result.GetPointLocationResult
import com.neki.map.application.result.GetPolygonLocationResult
import org.springframework.stereotype.Component

/**
 * fileName       : MapResultConverter
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : Map Result Converter
 */
@Component
class MapResultConverter(private val appProperties: AppProperties) {
    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toGetBrandResponse(result: List<GetBrandResult>): List<GetBrandResponse> = result.map {
        GetBrandResponse(
            id = it.id,
            name = it.name,
            code = it.code,
            imageUrl = it.storageKey?.let { key -> toImageUrl(key) },
        )
    }

    private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"

    fun toCollectPhotoBoothResponse(result: CollectPhotoBoothResult): CollectPhotoBoothResponse =
        CollectPhotoBoothResponse(
            collectedCount = result.collectedCount,
            duplicatedCount = result.duplicatedCount,
            totalProcessed = result.totalProcessed,
        )

    fun toGetPolygonLocationResponse(result: GetPolygonLocationResult): GetPolygonLocationResponse {
        val items: List<GetPolygonLocationResponse.PhotoBoothLocationInfo> = result.locations.map {
            GetPolygonLocationResponse.PhotoBoothLocationInfo(
                id = it.id,
                brandName = it.brandName,
                branchName = it.branchName,
                address = it.address,
                longitude = it.location.x,
                latitude = it.location.y,
                favorite = it.id in result.favoriteLocationIds,
            )
        }
        return GetPolygonLocationResponse(items = items)
    }

    fun toGetFavoriteMapResponse(result: GetFavoriteMapResult): GetFavoriteMapResponse {
        val items: List<GetFavoriteMapResponse.PhotoBoothLocationInfo> = result.locations.map {
            GetFavoriteMapResponse.PhotoBoothLocationInfo(
                id = it.id,
                brandName = it.brandName,
                branchName = it.branchName,
                address = it.address,
                longitude = it.location.x,
                latitude = it.location.y,
            )
        }
        return GetFavoriteMapResponse(items = items)
    }

    fun toGetPointLocationResponse(result: GetPointLocationResult): GetPointLocationResponse {
        val items: List<GetPointLocationResponse.PhotoBoothLocationWithDistanceInfo> = result.locations.map {
            GetPointLocationResponse.PhotoBoothLocationWithDistanceInfo(
                id = it.id,
                brandName = it.brandName,
                branchName = it.branchName,
                address = it.address,
                longitude = it.location.x,
                latitude = it.location.y,
                distance = it.distance,
                favorite = it.id in result.favoriteLocationIds,
            )
        }
        return GetPointLocationResponse(items = items)
    }
}
