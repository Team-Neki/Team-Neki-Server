package com.yapp2app.map.api.converter

import com.yapp2app.map.api.dto.CollectPhotoBoothRequest
import com.yapp2app.map.api.dto.GetPointLocationRequest
import com.yapp2app.map.api.dto.GetPolygonLocationRequest
import com.yapp2app.map.application.command.CollectPhotoBoothCommand
import com.yapp2app.map.application.command.GetPointLocationCommand
import com.yapp2app.map.application.command.GetPolygonLocationCommand
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

    fun toGetPolygonLocationCommand(request: GetPolygonLocationRequest): GetPolygonLocationCommand {
        val coordinates = request.coordinates.map { Coordinate(it.longitude!!, it.latitude!!) }
        return GetPolygonLocationCommand(
            coordinates = coordinates,
            brandIds = request.brandIds,
        )
    }

    fun toGetPointLocationCommand(request: GetPointLocationRequest): GetPointLocationCommand = GetPointLocationCommand(
        coordinate = Coordinate(request.longitude ?: GANGNAM_LONGITUDE, request.latitude ?: GANGNAM_LATITUDE),
        radiusInMeters = request.radiusInMeters,
        brandIds = request.brandIds,
    )
}
