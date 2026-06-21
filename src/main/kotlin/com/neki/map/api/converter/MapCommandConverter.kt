package com.neki.map.api.converter

import com.neki.map.api.dto.CollectPhotoBoothRequest
import com.neki.map.api.dto.GetPointLocationRequest
import com.neki.map.api.dto.GetPolygonLocationRequest
import com.neki.map.application.command.CollectPhotoBoothCommand
import com.neki.map.application.command.GetPointLocationCommand
import com.neki.map.application.command.GetPolygonLocationCommand
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

    fun toCollectPhotoBoothCommand(request: CollectPhotoBoothRequest): CollectPhotoBoothCommand =
        CollectPhotoBoothCommand(keyword = request.keyword!!, brandCode = request.brandCode!!)

    fun toGetPolygonLocationCommand(userId: Long, request: GetPolygonLocationRequest): GetPolygonLocationCommand {
        val coordinates: List<Coordinate> = request.coordinates.map { Coordinate(it.longitude!!, it.latitude!!) }
        return GetPolygonLocationCommand(
            userId = userId,
            coordinates = coordinates,
            brandIds = request.brandIds,
        )
    }

    fun toGetPointLocationCommand(userId: Long, request: GetPointLocationRequest): GetPointLocationCommand =
        GetPointLocationCommand(
            userId = userId,
            coordinate = Coordinate(request.longitude ?: GANGNAM_LONGITUDE, request.latitude ?: GANGNAM_LATITUDE),
            radiusInMeters = request.radiusInMeters,
            brandIds = request.brandIds,
        )
}
