package com.yapp2app.map.application.command

import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : MapCommand
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : Map domain command
 */
data class CollectPhotoBoothCommand(val keyword: String, val brandCode: String)

data class GetPolygonLocationCommand(val coordinates: List<Coordinate>, val brandIds: List<Long>?)

data class GetPointLocationCommand(
    val coordinate: Coordinate,
    val radiusInMeters: Int = 1000,
    val brandIds: List<Long>?,
)
