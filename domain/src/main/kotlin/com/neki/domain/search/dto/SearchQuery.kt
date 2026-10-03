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
     * 지역 검색. keyword 는 법정동 최하위 계층 이름 또는 전체 경로에 대한 접두 일치다.
     */
    data class SearchRegions(val keyword: String, val pagination: Pagination)

    /**
     * 지하철역 검색. keyword 는 역명 또는 `역명역 노선명` 에 대한 접두 일치다. 한 역이 노선 수만큼 나온다.
     */
    data class SearchStations(val keyword: String, val pagination: Pagination)

    /**
     * 부스 목록과 필터가 같이 쓰는 범위. keyword 는 자동완성 keyword 이거나 자치구·역 + 브랜드 검색어이며 QU 가 이해한다.
     * brandIds 가 null 이거나 비어 있으면 모든 브랜드.
     */
    interface PhotoBoothScope {
        val keyword: String
        val brandIds: List<Long>?
    }

    /**
     * 고른 지역·역의 부스 목록.
     */
    data class GetPhotoBooths(
        val userId: Long,
        override val keyword: String,
        override val brandIds: List<Long>?,
        val userLocation: UserLocation?,
    ) : PhotoBoothScope

    /**
     * 부스 목록에서 쓸 수 있는 브랜드 필터. 범위는 [GetPhotoBooths] 와 같고 userLocation 만 쓰지 않는다.
     */
    data class GetFilter(val userId: Long, override val keyword: String, override val brandIds: List<Long>?) :
        PhotoBoothScope
}
