package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : GetSearchFilterUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:29
 * description    : 부스 목록에서 쓸 수 있는 브랜드 필터 조회 (mock)
 */
@UseCase
class GetSearchFilterUseCase {

    /** mock: query 와 무관하게 항상 같은 집계를 내려준다. 실제 구현은 사용자별 브랜드 정렬 순서를 따르고 mock 은 브랜드 ID 순. */
    @Suppress("UNUSED_PARAMETER")
    fun execute(query: SearchQuery.GetFilter): SearchResult.GetFilter {
        val brandFilter: List<SearchResult.GetFilter.BrandFilter> = SearchMockData.photoBooths
            .groupingBy { it.brand }
            .eachCount()
            .entries
            .sortedBy { it.key.id }
            .map { (brand, count) ->
                SearchResult.GetFilter.BrandFilter(id = brand.id, name = brand.name, code = brand.code, count = count)
            }

        return SearchResult.GetFilter(brandFilter = brandFilter)
    }
}
