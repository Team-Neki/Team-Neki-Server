package com.neki.domain.search.models

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * fileName       : UserLocation
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 사용자 현재 위치 (Search domain value object). 좌표 범위를 벗어나면 만들 수 없다.
 */
data class UserLocation(val latitude: Double, val longitude: Double) {

    init {
        // NaN 과 ±Infinity 는 범위 비교가 항상 false 라 여기서 같이 걸린다.
        if (latitude !in MIN_LATITUDE..MAX_LATITUDE || longitude !in MIN_LONGITUDE..MAX_LONGITUDE) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    /**
     * 이 위치에서 주어진 좌표까지의 거리(m). 지구를 반지름 6,371km 구로 근사한다(haversine).
     */
    fun distanceTo(latitude: Double, longitude: Double): Int {
        val deltaLat: Double = Math.toRadians(latitude - this.latitude)
        val deltaLon: Double = Math.toRadians(longitude - this.longitude)
        val a: Double = sin(deltaLat / 2).pow(2) +
            cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(latitude)) * sin(deltaLon / 2).pow(2)
        return (2 * EARTH_RADIUS_METERS * asin(sqrt(a))).roundToInt()
    }

    companion object {
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
