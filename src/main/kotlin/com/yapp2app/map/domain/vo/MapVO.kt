package com.yapp2app.map.domain.vo

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.Coordinate as JtsCoordinate

/**
 * fileName       : MapVo
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : Map 관련 VO
 */

/**
 * 지리적 좌표를 나타내는 Value Object
 */
data class CoordinateVO(
    val longitude: Double, // 경도
    val latitude: Double, // 위도
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    companion object {
        private const val SRID = 4326
        private val geometryFactory = GeometryFactory(PrecisionModel(), SRID)
    }

    /**
     * 경도/위도를 Point 객체로 변환
     */
    fun toPoint(): Point = geometryFactory.createPoint(JtsCoordinate(longitude, latitude))
}

/**
 * 지리적 경계 범위를 나타내는 Value Object
 */
data class GeographicBoundsVO(val minCoordinateVO: CoordinateVO, val maxCoordinateVO: CoordinateVO) {
    init {
        require(minCoordinateVO.latitude < maxCoordinateVO.latitude) { "minLatitude must be less than maxLatitude" }
        require(minCoordinateVO.longitude < maxCoordinateVO.longitude) { "minLongitude must be less than maxLongitude" }
        require(minCoordinateVO.latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(maxCoordinateVO.latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(minCoordinateVO.longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(maxCoordinateVO.longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    companion object {
        private const val DEFAULT_GRID_SIZE = 0.1 // 약 10km

        /**
         * 대한민국 지리적 경계
         */
        val KOREA = GeographicBoundsVO(
            CoordinateVO(
                latitude = 33.0, // 최남단 (제주 마라도 근처)
                longitude = 124.5, // 최서단 (백령도 근처)
            ),
            CoordinateVO(
                latitude = 38.6, // 최북단 (DMZ 근처)
                longitude = 132.0, // 최동단 (독도 근처)
            ),
        )
    }

    /**
     * 현재 경계 영역을 그리드로 분할
     * @param gridSize 그리드 크기 (도 단위, 예: 0.1 = 약 10km)
     * @return 그리드 사각형 리스트
     */
    fun divideIntoGrids(gridSize: Double = DEFAULT_GRID_SIZE): List<GeographicRectVO> {
        require(gridSize > 0) { "Grid size must be positive" }

        val grids = mutableListOf<GeographicRectVO>()

        var lat = minCoordinateVO.latitude
        while (lat < maxCoordinateVO.latitude) {
            var lng = minCoordinateVO.longitude
            while (lng < maxCoordinateVO.longitude) {
                val southWest = CoordinateVO(longitude = lng, latitude = lat)
                val northEast = CoordinateVO(
                    longitude = (lng + gridSize).coerceAtMost(maxCoordinateVO.longitude),
                    latitude = (lat + gridSize).coerceAtMost(maxCoordinateVO.latitude),
                )
                grids.add(GeographicRectVO(southWest, northEast))
                lng += gridSize
            }
            lat += gridSize
        }

        return grids
    }
}

/**
 * 지리적 사각형 영역을 나타내는 Value Object
 */
data class GeographicRectVO(
    val southWest: CoordinateVO, // 좌하단 좌표
    val northEast: CoordinateVO, // 우상단 좌표
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
