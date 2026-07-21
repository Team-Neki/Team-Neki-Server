package com.neki.map.api.converter

import com.neki.map.api.dto.CollectPhotoBoothRequest
import com.neki.map.api.dto.GetPointLocationRequest
import com.neki.map.api.dto.GetPolygonLocationRequest
import com.neki.map.api.dto.UpdateBrandOrderRequest
import com.neki.map.application.dto.MapCommand
import com.neki.map.application.dto.MapQuery
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component

/**
 * fileName       : MapCommandConverter
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : Map Command Converter
 */
@Component
class MapCommandConverter {

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
