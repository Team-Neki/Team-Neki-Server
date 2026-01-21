package com.yapp2app.map.application.result

import com.yapp2app.map.application.contract.PhotoBoothLocationDto
import com.yapp2app.map.application.contract.PhotoBoothLocationWithDistanceDto

/**
 * fileName       : MapResult
 * author         : darren
 * date           : 2026. 1. 14. 13:31
 * description    :
 */
data class GetBrandResult(val id: Long, val name: String, val code: String, val imageUrl: String?)

data class PhotoBoothResult(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

data class GetPolygonLocationResult(val locations: List<PhotoBoothLocationDto>, val hasNext: Boolean)

data class GetPointLocationResult(val locations: List<PhotoBoothLocationWithDistanceDto>, val hasNext: Boolean)
