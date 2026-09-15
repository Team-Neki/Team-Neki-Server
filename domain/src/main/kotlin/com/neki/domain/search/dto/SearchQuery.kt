package com.neki.domain.search.dto

import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.UserLocation

/**
 * fileName       : SearchQuery
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain query
 */
object SearchQuery {
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
