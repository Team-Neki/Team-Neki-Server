package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SearchTarget

/**
 * fileName       : SearchStationsUseCase
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 지하철역 검색 (mock)
 */
@UseCase
class SearchStationsUseCase {

    fun execute(query: SearchQuery.SearchStations): SearchResult.SearchStations {
        val matched: List<SearchTarget.Station> = SearchMockData.searchStations(query.keyword)

        val page: Page<SearchTarget.Station> = query.pagination.let {
            it.slice(matched.drop(it.offset).take(it.limit))
        }

        val items: List<SearchResult.SearchStations.Item> = page.items.map {
            SearchResult.SearchStations.Item(name = it.name, lineName = it.lineName)
        }

        return SearchResult.SearchStations(
            items = items,
            hasNext = page.hasNext,
            totalCount = matched.size.toLong(),
        )
    }
}
