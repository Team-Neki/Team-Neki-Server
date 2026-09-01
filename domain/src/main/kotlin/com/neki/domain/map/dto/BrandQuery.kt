package com.neki.domain.map.dto

import com.neki.core.domain.vo.Pagination

/**
 * fileName       : GetBrandsQuery
 * author         : koo
 * date           : 2026. 8. 8. 오후 5:58
 * description    :
 */
object BrandQuery {
    /**
     * supportsQr, exposeToMap 은 null 이면 해당 조건으로 거르지 않는다.
     */
    data class GetBrands(val supportsQr: Boolean?, val exposeToMap: Boolean?, val pagination: Pagination)

    data class SearchBrands(val keyword: String, val pagination: Pagination)
}
