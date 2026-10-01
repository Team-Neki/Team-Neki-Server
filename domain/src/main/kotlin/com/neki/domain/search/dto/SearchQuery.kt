package com.neki.domain.search.dto

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.UserLocation

/**
 * fileName       : SearchQuery
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain query
 */
object SearchQuery {

    /** 지역·역 자동완성 검색어의 최소 길이. 1자는 걸리는 것이 너무 많아 조회하지 않고 빈 결과를 준다 */
    const val MIN_COMPLETION_KEYWORD_LENGTH = 2

    /**
     * 지역 검색. keyword 는 법정동 최하위 계층 이름 또는 전체 경로에 대한 접두 일치다.
     */
    data class SearchRegions(val keyword: String, val pagination: Pagination)

    /**
     * 지하철역 검색. keyword 는 역명 또는 `역명역 노선명` 에 대한 접두 일치다. 한 역이 노선 수만큼 나온다.
     * userLocation 은 정렬에 쓰지 않고 각 역까지의 거리를 내려주는 데만 쓴다.
     */
    data class SearchStations(val keyword: String, val pagination: Pagination, val userLocation: UserLocation?)

    /**
     * 부스 검색. keyword 는 지점명, 브랜드명, `브랜드명 지점명` 에 대한 접두 일치다.
     * userLocation 이 있으면 가까운 순, 없으면 브랜드명, 지점명 순.
     */
    data class SearchPhotoBoothsByKeyword(
        val keyword: String,
        val pagination: Pagination,
        val userLocation: UserLocation?,
    )

    /**
     * 고른 지역·역의 부스 목록. brandIds 가 null 이거나 비어 있으면 모든 브랜드.
     */
    data class GetPhotoBooths(
        val userId: Long,
        val target: SearchTarget,
        val brandIds: List<Long>?,
        val userLocation: UserLocation?,
    )

    /**
     * 부스 목록에서 쓸 수 있는 브랜드 필터. 요청 body 는 [GetPhotoBooths] 와 같고 userLocation 만 쓰지 않는다.
     */
    data class GetFilter(val userId: Long, val target: SearchTarget, val brandIds: List<Long>?)
}
