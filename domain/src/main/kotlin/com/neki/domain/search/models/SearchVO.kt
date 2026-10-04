package com.neki.domain.search.models

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * fileName       : SearchVO
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain value objects
 */

/**
 * 검색어가 가리키는 대상. 지역·역은 부스 목록의 범위(Area)가 되고, 브랜드는 그 범위 안을 거르는 조건이다.
 */
sealed interface SearchTarget {
    /** 혼자서 부스 목록의 범위가 되는 대상. 지역·역(Area) 또는 지점 하나(Booth) */
    sealed interface Scope : SearchTarget

    /** 지역·역. NER 이 검색어 안에서 찾는 범위다 */
    sealed interface Area : Scope

    /** 법정동코드 10자리 */
    data class Region(val code: String) : Area

    /** 역명(`역` 접미사 없음)과 노선명. 한 역이 노선마다 따로 존재한다. */
    data class Station(val name: String, val lineName: String) : Area

    /**
     * 지점 하나. 검색 색인 행의 원천 키라 색인을 다시 만들어도 같은 지점을 가리킨다.
     * 부스 자동완성 keyword(`브랜드명 지점명`)로만 정해진다. NER 사전에도 이 이름으로 들어 있다.
     */
    data class Booth(val platform: String, val idx: String) : Scope

    /** 브랜드. 범위가 아니라 범위 안을 거르는 조건이다 */
    data class Brand(val brandId: Long) : SearchTarget
}

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

/**
 * 검색 색인 행과 원천 키 (platform, idx) 가 같은 지도 부스(map 의 TB_PHOTO_BOOTH_LOCATION). 색인 행 id 는 색인 세대마다 바뀌므로
 * 응답 id 와 즐겨찾기는 이 값을 쓴다.
 */
data class MapBooth(val platform: String, val idx: String, val locationId: Long, val favorite: Boolean)
