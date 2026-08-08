package com.neki.domain.map

import com.neki.domain.map.models.Brand

/**
 * fileName       : BrandOrderPolicy
 * author         : darren
 * date           : 2026. 7. 13.
 * description    : 사용자별 브랜드 정렬 규칙.
 *   사용자가 정렬을 커스텀한 브랜드는 sortOrder 순으로, 그 외(저장 이후 추가된 브랜드 등)는 뒤쪽에 id 순으로 정렬한다.
 *   저장된 순서가 없으면 모두 동일하게 취급되어 조회 기본 정렬(id 오름차순)을 따른다.
 */
object BrandOrderPolicy {

    fun sort(brands: List<Brand>, sortOrderMap: Map<Long, Int>): List<Brand> = brands.sortedWith(
        compareBy(
            { sortOrderMap[it.id] ?: Int.MAX_VALUE },
            { it.id },
        ),
    )
}
