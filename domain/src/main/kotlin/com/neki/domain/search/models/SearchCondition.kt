package com.neki.domain.search.models

import com.neki.domain.search.models.qu.QueryIntent

/**
 * fileName       : SearchCondition
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : QU 가 이해한 대상과 요청 필터를 부스 목록 조회 조건으로 바꾼 것.
 *   지역·역(areas)은 범위이고 브랜드(brandIds)는 범위 안을 거르는 조건이다. brandIds 가 null 이면 모든 브랜드.
 *   검색어의 브랜드와 요청의 브랜드 필터는 둘 다 만족해야 한다. 지점(BRANCH)과 남은 조각은 조건이 되지 않는다.
 */
class SearchCondition private constructor(val areas: List<SearchTarget.Area>, val brandIds: List<Long>?) {

    /** 검색어의 브랜드와 요청 필터가 겹치지 않아 어떤 부스도 맞을 수 없다 */
    val matchesNothing: Boolean
        get() = brandIds?.isEmpty() == true

    companion object {
        /** 범위(지역·역)를 하나도 인식하지 못했으면 null */
        fun of(intent: QueryIntent, requestedBrandIds: List<Long>?): SearchCondition? {
            val areas: List<SearchTarget.Area> = intent.targets.filterIsInstance<SearchTarget.Area>()
            if (areas.isEmpty()) return null

            val recognized: List<Long> = intent.targets.filterIsInstance<SearchTarget.Brand>().map { it.brandId }

            // 둘 다 없으면 null(모든 브랜드). 둘 다 있으면 교집합이라 빈 목록일 수 있다
            val brandIds: List<Long>? = when {
                requestedBrandIds.isNullOrEmpty() -> recognized.ifEmpty { null }
                recognized.isEmpty() -> requestedBrandIds
                else -> requestedBrandIds.filter { it in recognized }
            }

            return SearchCondition(areas, brandIds)
        }
    }
}
