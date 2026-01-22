package com.yapp2app.map.application.command

/**
 * fileName       : MapCommand
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : Map domain command
 */
data class CollectPhotoBoothCommand(val keyword: String, val brandCode: String)

data class GetPolygonLocationCommand(
    val coordinates: List<Pair<Double, Double>>,
    val brandIds: List<Long>?,
    val page: Int = 0,
    val size: Int = 20,
)

data class GetPointLocationCommand(
    val longitude: Double,
    val latitude: Double,
    val radiusInMeters: Int = 1000,
    val brandIds: List<Long>?,
    val page: Int = 0,
    val size: Int = 20,
)
