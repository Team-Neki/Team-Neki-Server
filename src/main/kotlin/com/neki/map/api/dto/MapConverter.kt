package com.neki.map.api.dto

import com.neki.common.properties.AppProperties
import com.neki.map.application.dto.MapCommand
import com.neki.map.application.dto.MapQuery
import com.neki.map.application.dto.MapResult
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component

/**
 * fileName       : MapConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map api layer converter
 */
object MapConverter {
    @Component
    class RequestConverter {
        companion object {
            const val GANGNAM_LONGITUDE = 127.0276
            const val GANGNAM_LATITUDE = 37.4979
        }

        fun toCollectPhotoBoothCommand(request: CollectPhotoBoothRequest): MapCommand.CollectPhotoBooth =
            MapCommand.CollectPhotoBooth(keyword = request.keyword!!, brandCode = request.brandCode!!)

        fun toGetPolygonLocationQuery(userId: Long, request: GetPolygonLocationRequest): MapQuery.GetPolygonLocation {
            val coordinates: List<Coordinate> = request.coordinates.map { Coordinate(it.longitude!!, it.latitude!!) }
            return MapQuery.GetPolygonLocation(
                userId = userId,
                coordinates = coordinates,
                brandIds = request.brandIds,
            )
        }

        fun toGetPointLocationQuery(userId: Long, request: GetPointLocationRequest): MapQuery.GetPointLocation =
            MapQuery.GetPointLocation(
                userId = userId,
                coordinate = Coordinate(request.longitude ?: GANGNAM_LONGITUDE, request.latitude ?: GANGNAM_LATITUDE),
                radiusInMeters = request.radiusInMeters,
                brandIds = request.brandIds,
            )

        fun toUpdateBrandOrderCommand(userId: Long, request: UpdateBrandOrderRequest): MapCommand.UpdateBrandOrder =
            MapCommand.UpdateBrandOrder(
                userId = userId,
                brandIds = request.brandIds,
            )
    }

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
        }

        fun toGetBrandResponse(result: List<MapResult.GetBrand>): List<GetBrandResponse> = result.map {
            GetBrandResponse(
                id = it.id,
                name = it.name,
                code = it.code,
                imageUrl = it.storageKey?.let { key -> toImageUrl(key) },
            )
        }

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"

        fun toCollectPhotoBoothResponse(result: MapResult.CollectPhotoBooth): CollectPhotoBoothResponse =
            CollectPhotoBoothResponse(
                collectedCount = result.collectedCount,
                duplicatedCount = result.duplicatedCount,
                totalProcessed = result.totalProcessed,
            )

        fun toGetPolygonLocationResponse(result: MapResult.GetPolygonLocation): GetPolygonLocationResponse {
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

        fun toGetFavoriteMapResponse(result: MapResult.GetFavoriteMap): GetFavoriteMapResponse {
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

        fun toGetPointLocationResponse(result: MapResult.GetPointLocation): GetPointLocationResponse {
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
}
