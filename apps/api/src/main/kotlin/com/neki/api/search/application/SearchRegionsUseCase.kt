package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : SearchRegionsUseCase
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 지역 검색 (mock)
 */
@UseCase
class SearchRegionsUseCase {

    fun execute(query: SearchQuery.SearchRegions): SearchResult.SearchRegions {
        val matched: List<SearchMockData.Region> = SearchMockData.searchRegions(query.keyword)

        val page: Page<SearchMockData.Region> = query.pagination.let {
            it.slice(matched.drop(it.offset).take(it.limit))
        }

        val items: List<SearchResult.SearchRegions.Item> = page.items.map {
            SearchResult.SearchRegions.Item(
                code = it.code,
                level = it.level,
                name = it.name,
                fullName = it.fullName,
            )
        }

        return SearchResult.SearchRegions(
            items = items,
            hasNext = page.hasNext,
            totalCount = matched.size.toLong(),
        )
    }
}
