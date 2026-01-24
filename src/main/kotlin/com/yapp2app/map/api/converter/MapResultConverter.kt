package com.yapp2app.map.api.converter

import com.yapp2app.common.properties.AppProperties
import com.yapp2app.map.api.dto.CollectPhotoBoothResponse
import com.yapp2app.map.api.dto.GetBrandResponse
import com.yapp2app.map.api.dto.GetPointLocationResponse
import com.yapp2app.map.api.dto.GetPolygonLocationResponse
import com.yapp2app.map.application.result.CollectPhotoBoothResult
import com.yapp2app.map.application.result.GetBrandResult
import com.yapp2app.map.application.result.GetPointLocationResult
import com.yapp2app.map.application.result.GetPolygonLocationResult
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
        val items = result.locations.map {
            GetPolygonLocationResponse.PhotoBoothLocationInfo(
                id = it.id,
                brandId = it.brandId,
                name = it.name,
                address = it.address,
                longitude = it.location.x,
                latitude = it.location.y,
            )
        }
        return GetPolygonLocationResponse(items = items)
    }

    fun toGetPointLocationResponse(result: GetPointLocationResult): GetPointLocationResponse {
        val items = result.locations.map {
            GetPointLocationResponse.PhotoBoothLocationWithDistanceInfo(
                id = it.id,
                brandId = it.brandId,
                name = it.name,
                address = it.address,
                longitude = it.location.x,
                latitude = it.location.y,
                distance = it.distance,
            )
        }
        return GetPointLocationResponse(items = items)
    }
}
