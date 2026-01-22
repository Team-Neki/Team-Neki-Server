package com.yapp2app.map.api.converter

import com.yapp2app.map.api.dto.CollectPhotoBoothRequest
import com.yapp2app.map.api.dto.GetPointLocationRequest
import com.yapp2app.map.api.dto.GetPolygonLocationRequest
import com.yapp2app.map.application.command.CollectPhotoBoothCommand
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

    fun toCollectPhotoBoothCommand(request: CollectPhotoBoothRequest): CollectPhotoBoothCommand =
        CollectPhotoBoothCommand(keyword = request.keyword, brandCode = request.brandCode)

    fun toGetPolygonLocationCommand(request: GetPolygonLocationRequest): GetPolygonLocationCommand {
        val coordinates = request.coordinates.map { it.longitude to it.latitude }
        return GetPolygonLocationCommand(
            coordinates = coordinates,
            brandIds = request.brandIds,
            page = request.page,
            size = request.size,
        )
    }

    fun toGetPointLocationCommand(request: GetPointLocationRequest): GetPointLocationCommand = GetPointLocationCommand(
        longitude = request.longitude,
        latitude = request.latitude,
        radiusInMeters = request.radiusInMeters,
        brandIds = request.brandIds,
        page = request.page,
        size = request.size,
    )
}
