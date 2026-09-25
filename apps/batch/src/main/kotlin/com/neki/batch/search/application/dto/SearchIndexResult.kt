package com.neki.batch.search.application.dto

/**
 * fileName       : SearchIndexResult
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 재생성 결과. 잡이 로그로 남긴다
 */
data class SearchIndexResult(
    val indexed: Int,
    val stationLinks: Int,
    val skippedNoCoordinate: Int,
    val skippedNoBrand: Map<String, Int>,
)
