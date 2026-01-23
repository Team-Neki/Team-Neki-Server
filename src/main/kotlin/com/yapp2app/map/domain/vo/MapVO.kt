package com.yapp2app.map.domain.vo

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : MapVo
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : Map 관련 VO
 */

/**
 * 지리적 좌표를 나타내는 Value Object
 */
data class GeoPoint(val point: Point) {
    companion object {
        private const val SRID = 4326
        private val geometryFactory = GeometryFactory(PrecisionModel(), SRID)

        fun of(longitude: Double, latitude: Double): GeoPoint {
            require(longitude in -180.0..180.0) {
                "longitude must be between -180 and 180"
            }
            require(latitude in -90.0..90.0) {
                "latitude must be between -90 and 90"
            }

            val point = geometryFactory.createPoint(Coordinate(longitude, latitude))

            return GeoPoint(point)
        }
    }
}

/**
 * 지리적 경계 범위를 나타내는 Value Object
 */
data class GeographicKoreaBoundsVO(val southWest: Coordinate, val northEast: Coordinate) {

    companion object {
        private const val DEFAULT_GRID_SIZE = 0.1 // 약 10km
        private const val KOREA_MIN_LATITUDE = 33.0 // 최남단 (제주 마라도 근처)
        private const val KOREA_MIN_LONGITUDE = 124.5 // 최서단 (백령도 근처)
        private const val KOREA_MAX_LATITUDE = 38.6 // 최북단 (DMZ 근처)
        private const val KOREA_MAX_LONGITUDE = 132.0 // 최동단 (독도 근처)

        /**
         * 현재 경계 영역을 그리드로 분할
         * @param gridSize 그리드 크기 (도 단위, 예: 0.1 = 약 10km)
         * @return 그리드 사각형 리스트
         */
        fun divideIntoGrids(): List<GeographicKoreaBoundsVO> {
            val grids = mutableListOf<GeographicKoreaBoundsVO>()

            var lat = KOREA_MIN_LATITUDE
            while (lat < KOREA_MAX_LATITUDE) {
                var lng = KOREA_MIN_LONGITUDE
                while (lng < KOREA_MAX_LONGITUDE) {
                    val bound = GeographicKoreaBoundsVO(
                        southWest = Coordinate(lng, lat),
                        northEast = Coordinate(
                            (lng + DEFAULT_GRID_SIZE).coerceAtMost(KOREA_MAX_LONGITUDE),
                            (lat + DEFAULT_GRID_SIZE).coerceAtMost(KOREA_MAX_LATITUDE),
                        ),
                    )
                    grids.add(bound)
                    lng += DEFAULT_GRID_SIZE
                }
                lat += DEFAULT_GRID_SIZE
            }

            return grids
        }
    }

    /**
     * 카카오 API rect 파라미터 형식으로 변환
     * @return "x1,y1,x2,y2" 형식 (좌하단 경도,위도,우상단 경도,위도)
     */
    fun toRectString(): String = "${southWest.x},${southWest.y},${northEast.x},${northEast.y}"
}
