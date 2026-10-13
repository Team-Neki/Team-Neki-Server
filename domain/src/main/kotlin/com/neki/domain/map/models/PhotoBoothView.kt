package com.neki.domain.map.models

import org.locationtech.jts.geom.Point
import java.time.LocalDateTime

/**
 * fileName       : PhotoBoothView
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 브랜드명을 붙인 포토부스(enriched) 조회 결과. 지도 V2 가 쓴다
 */
data class PhotoBoothView(
    val key: PhotoBoothKey,
    val brandName: String,
    val branchName: String,
    val address: String?,
    val location: Point,
)

/**
 * 기준 좌표로부터의 거리(m)를 함께 담은 포토부스(enriched) 조회 결과
 */
data class PhotoBoothWithDistance(
    val key: PhotoBoothKey,
    val brandName: String,
    val branchName: String,
    val address: String?,
    val location: Point,
    val distance: Int,
)

/**
 * 즐겨찾기한 부스와 즐겨찾기한 시각. 두 즐겨찾기 테이블을 합칠 때 순서를 정하는 데 쓴다
 */
data class FavoritedPhotoBooth(val key: PhotoBoothKey, val favoritedAt: LocalDateTime)

/**
 * 즐겨찾기한 지점 마스터 위치와 즐겨찾기한 시각. 지도 V1 이 두 즐겨찾기 테이블을 합칠 때 쓴다
 */
data class FavoritedLocation(val location: PhotoBoothLocationView, val favoritedAt: LocalDateTime)
