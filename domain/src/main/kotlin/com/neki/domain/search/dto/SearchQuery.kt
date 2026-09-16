package com.neki.domain.search.dto

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.UserLocation

/**
 * fileName       : SearchQuery
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain query
 */
object SearchQuery {

    /**
     * 지역 검색. keyword 는 법정동 최하위 계층 이름에 대한 접두 일치다.
     */
    data class SearchRegions(val keyword: String, val pagination: Pagination)

    /**
     * 지하철역 검색. 한 역이 노선 수만큼 나온다.
     */
    data class SearchStations(val keyword: String, val pagination: Pagination)

    /**
     * 부스 검색. keyword 는 지점명에 대한 접두 일치이고 브랜드명·주소는 보지 않는다.
     * userLocation 이 있으면 distance 를 채우고 가까운 순으로 정렬한다.
     */
    data class SearchPhotoBoothsByKeyword(
        val userId: Long,
        val keyword: String,
        val pagination: Pagination,
        val userLocation: UserLocation?,
    )

    /**
     * 검색어에 맞는 부스 목록. brandIds 가 null 이거나 비어 있으면 모든 브랜드.
     */
    data class GetPhotoBooths(
        val userId: Long,
        val keyword: String,
        val brandIds: List<Long>?,
        val userLocation: UserLocation?,
    )

    /**
     * 부스 목록에서 쓸 수 있는 브랜드 필터. 검색어와 브랜드 필터는 [GetPhotoBooths] 와 같고 userLocation 만 쓰지 않는다.
     */
    data class GetFilter(val userId: Long, val keyword: String, val brandIds: List<Long>?)
}
