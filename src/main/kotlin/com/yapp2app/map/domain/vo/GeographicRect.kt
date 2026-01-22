package com.yapp2app.map.domain.vo

/**
 * fileName       : GeographicRect
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : 지리적 사각형 영역을 나타내는 Value Object
 */
data class GeographicRect(
    val southWest: Coordinate, // 좌하단 좌표
    val northEast: Coordinate, // 우상단 좌표
) {
    init {
        require(southWest.latitude < northEast.latitude) {
            "South latitude must be less than north latitude"
        }
        require(southWest.longitude < northEast.longitude) {
            "West longitude must be less than east longitude"
        }
    }

    /**
     * 카카오 API rect 파라미터 형식으로 변환
     * @return "x1,y1,x2,y2" 형식 (좌하단 경도,위도,우상단 경도,위도)
     */
    fun toRectString(): String =
        "${southWest.longitude},${southWest.latitude},${northEast.longitude},${northEast.latitude}"
}

/**
 * 지리적 좌표를 나타내는 Value Object
 */
data class Coordinate(
    val longitude: Double, // 경도
    val latitude: Double, // 위도
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}
