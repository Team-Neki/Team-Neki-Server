package com.yapp2app.map.api.converter

import com.yapp2app.map.api.dto.GetPointLocationResponse
import com.yapp2app.map.api.dto.GetPolygonLocationResponse
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
class MapResultConverter {

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
        return GetPolygonLocationResponse(items = items, hasNext = result.hasNext)
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
        return GetPointLocationResponse(items = items, hasNext = result.hasNext)
    }
}
