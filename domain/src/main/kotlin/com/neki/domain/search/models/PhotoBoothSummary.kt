package com.neki.domain.search.models

/**
 * fileName       : PhotoBoothSummary
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 부스 검색 자동완성에 쓰는 포토부스 이름과 좌표. map 도메인의 포토부스를 SearchMapClient 가 옮겨 담는다.
 */
data class PhotoBoothSummary(
    val brandName: String,
    val branchName: String,
    val latitude: Double,
    val longitude: Double,
)
