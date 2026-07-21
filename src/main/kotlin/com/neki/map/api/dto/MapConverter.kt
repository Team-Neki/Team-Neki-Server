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

        fun toCollectPhotoBoothCommand(request: MapRequest.CollectPhotoBooth): MapCommand.CollectPhotoBooth =
            MapCommand.CollectPhotoBooth(keyword = request.keyword!!, brandCode = request.brandCode!!)

        fun toGetPolygonLocationQuery(
            userId: Long,
            request: MapRequest.GetPolygonLocation,
        ): MapQuery.GetPolygonLocation {
            val coordinates: List<Coordinate> = request.coordinates.map { Coordinate(it.longitude!!, it.latitude!!) }
            return MapQuery.GetPolygonLocation(
                userId = userId,
                coordinates = coordinates,
                brandIds = request.brandIds,
            )
        }

        fun toGetPointLocationQuery(userId: Long, request: MapRequest.GetPointLocation): MapQuery.GetPointLocation =
            MapQuery.GetPointLocation(
                userId = userId,
                coordinate = Coordinate(request.longitude ?: GANGNAM_LONGITUDE, request.latitude ?: GANGNAM_LATITUDE),
                radiusInMeters = request.radiusInMeters,
                brandIds = request.brandIds,
            )

        fun toUpdateBrandOrderCommand(userId: Long, request: MapRequest.UpdateBrandOrder): MapCommand.UpdateBrandOrder =
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

        fun toGetBrandResponse(result: List<MapResult.GetBrand>): List<MapResponse.GetBrand> = result.map {
            MapResponse.GetBrand(
                id = it.id,
                name = it.name,
                code = it.code,
                imageUrl = it.storageKey?.let { key -> toImageUrl(key) },
            )
        }

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$storageKey"

        fun toCollectPhotoBoothResponse(result: MapResult.CollectPhotoBooth): MapResponse.CollectPhotoBooth =
            MapResponse.CollectPhotoBooth(
                collectedCount = result.collectedCount,
                duplicatedCount = result.duplicatedCount,
                totalProcessed = result.totalProcessed,
            )

        fun toGetPolygonLocationResponse(result: MapResult.GetPolygonLocation): MapResponse.GetPolygonLocation {
            val items: List<MapResponse.GetPolygonLocation.PhotoBoothLocationInfo> = result.locations.map {
                MapResponse.GetPolygonLocation.PhotoBoothLocationInfo(
                    id = it.id,
                    brandName = it.brandName,
                    branchName = it.branchName,
                    address = it.address,
                    longitude = it.location.x,
                    latitude = it.location.y,
                    favorite = it.id in result.favoriteLocationIds,
                )
            }
            return MapResponse.GetPolygonLocation(items = items)
        }

        fun toGetFavoriteMapResponse(result: MapResult.GetFavoriteMap): MapResponse.GetFavoriteMap {
            val items: List<MapResponse.GetFavoriteMap.PhotoBoothLocationInfo> = result.locations.map {
                MapResponse.GetFavoriteMap.PhotoBoothLocationInfo(
                    id = it.id,
                    brandName = it.brandName,
                    branchName = it.branchName,
                    address = it.address,
                    longitude = it.location.x,
                    latitude = it.location.y,
                )
            }
            return MapResponse.GetFavoriteMap(items = items)
        }

        fun toGetPointLocationResponse(result: MapResult.GetPointLocation): MapResponse.GetPointLocation {
            val items: List<MapResponse.GetPointLocation.PhotoBoothLocationWithDistanceInfo> = result.locations.map {
                MapResponse.GetPointLocation.PhotoBoothLocationWithDistanceInfo(
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
            return MapResponse.GetPointLocation(items = items)
        }
    }
}
