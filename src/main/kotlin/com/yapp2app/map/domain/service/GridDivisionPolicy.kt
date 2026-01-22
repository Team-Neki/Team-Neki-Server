package com.yapp2app.map.domain.service

import com.yapp2app.map.domain.vo.Coordinate
import com.yapp2app.map.domain.vo.GeographicBounds
import com.yapp2app.map.domain.vo.GeographicRect
import org.springframework.stereotype.Component

/**
 * fileName       : GridDivisionPolicy
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : 지리적 영역을 그리드로 분할하는 도메인 정책
 */
@Component
class GridDivisionPolicy {
    companion object {
        private const val DEFAULT_GRID_SIZE = 0.1 // 약 10km
    }

    /**
     * 대한민국을 그리드로 분할
     * @param gridSize 그리드 크기 (도 단위, 예: 0.1 = 약 10km)
     * @return 그리드 사각형 리스트
     */
    fun divideKoreaIntoGrids(gridSize: Double = DEFAULT_GRID_SIZE): List<GeographicRect> =
        divideIntoGrids(GeographicBounds.KOREA, gridSize)

    /**
     * 지리적 영역을 그리드로 분할
     * @param bounds 분할할 영역
     * @param gridSize 그리드 크기 (도 단위)
     * @return 그리드 사각형 리스트
     */
    fun divideIntoGrids(bounds: GeographicBounds, gridSize: Double): List<GeographicRect> {
        require(gridSize > 0) { "Grid size must be positive" }

        val grids = mutableListOf<GeographicRect>()

        var lat = bounds.minLatitude
        while (lat < bounds.maxLatitude) {
            var lng = bounds.minLongitude
            while (lng < bounds.maxLongitude) {
                val southWest = Coordinate(longitude = lng, latitude = lat)
                val northEast = Coordinate(
                    longitude = (lng + gridSize).coerceAtMost(bounds.maxLongitude),
                    latitude = (lat + gridSize).coerceAtMost(bounds.maxLatitude),
                )
                grids.add(GeographicRect(southWest, northEast))
                lng += gridSize
            }
            lat += gridSize
        }

        return grids
    }
}
