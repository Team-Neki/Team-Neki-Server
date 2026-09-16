package com.neki.domain.search.models

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
 * description    : 사용자 현재 위치 (Search domain value object)
 */

/**
 * 사용자 현재 위치
 */
data class UserLocation(val latitude: Double, val longitude: Double) {

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
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
