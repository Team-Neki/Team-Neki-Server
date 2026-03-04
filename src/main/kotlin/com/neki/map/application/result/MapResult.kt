package com.neki.map.application.result

import com.neki.map.contract.PhotoBoothLocationDto
import com.neki.map.contract.PhotoBoothLocationWithDistanceDto

/**
 * fileName       : MapResult
 * author         : darren
 * date           : 2026. 1. 14. 13:31
 * description    :
 */
data class GetBrandResult(val id: Long, val name: String, val code: String, val storageKey: String?)

data class CollectPhotoBoothResult(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

data class PhotoBoothResult(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

data class GetPolygonLocationResult(val locations: List<PhotoBoothLocationDto>)

data class GetPointLocationResult(val locations: List<PhotoBoothLocationWithDistanceDto>)
