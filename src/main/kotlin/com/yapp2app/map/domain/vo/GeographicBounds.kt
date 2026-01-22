package com.yapp2app.map.domain.vo

/**
 * fileName       : GeographicBounds
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : 지리적 경계 범위를 나타내는 Value Object
 */
data class GeographicBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    init {
        require(minLatitude < maxLatitude) { "minLatitude must be less than maxLatitude" }
        require(minLongitude < maxLongitude) { "minLongitude must be less than maxLongitude" }
        require(minLatitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(maxLatitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(minLongitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(maxLongitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    companion object {
        /**
         * 대한민국 지리적 경계
         */
        val KOREA = GeographicBounds(
            minLatitude = 33.0, // 최남단 (제주 마라도 근처)
            maxLatitude = 38.6, // 최북단 (DMZ 근처)
            minLongitude = 124.5, // 최서단 (백령도 근처)
            maxLongitude = 132.0, // 최동단 (독도 근처)
        )
    }
}
