package com.yapp2app.map.api.converter

import com.yapp2app.map.api.dto.GetPointLocationRequest
import com.yapp2app.map.api.dto.GetPolygonLocationRequest
import com.yapp2app.map.application.command.GetPointLocationCommand
import com.yapp2app.map.application.command.GetPolygonLocationCommand
import org.springframework.stereotype.Component

/**
 * fileName       : MapCommandConverter
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : Map Command Converter
 */
@Component
class MapCommandConverter {

    fun toGetPolygonLocationCommand(request: GetPolygonLocationRequest): GetPolygonLocationCommand {
        val coordinates = request.coordinates.map { it.longitude to it.latitude }
        return GetPolygonLocationCommand(
            coordinates = coordinates,
            brandId = request.brandId,
            page = request.page,
            size = request.size,
        )
    }

    fun toGetPointLocationCommand(request: GetPointLocationRequest): GetPointLocationCommand = GetPointLocationCommand(
        longitude = request.longitude,
        latitude = request.latitude,
        radiusInMeters = request.radiusInMeters,
        brandId = request.brandId,
        page = request.page,
        size = request.size,
    )
}
