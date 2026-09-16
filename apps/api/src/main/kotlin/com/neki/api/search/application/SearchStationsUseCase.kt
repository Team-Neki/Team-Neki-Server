package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.Station

/**
 * fileName       : SearchStationsUseCase
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 지하철역 검색 (mock)
 */
@UseCase
class SearchStationsUseCase {

    fun execute(query: SearchQuery.SearchStations): SearchResult.Completion {
        val matched: List<Station> = SearchMockData.searchStations(query.keyword)

        val page: Page<Station> = query.pagination.let {
            it.slice(matched.drop(it.offset).take(it.limit))
        }

        // 저장된 역명에는 `역` 이 없다. 화면에 보일 `강남역 2호선` 형태로 맞춰 내려준다.
        return SearchResult.Completion(
            keywords = page.items.map { "${it.name}역 ${it.lineName}" },
            hasNext = page.hasNext,
            totalCount = matched.size.toLong(),
        )
    }
}
