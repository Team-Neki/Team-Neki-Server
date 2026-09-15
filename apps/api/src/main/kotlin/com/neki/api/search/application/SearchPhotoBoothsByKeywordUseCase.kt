package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : SearchPhotoBoothsByKeywordUseCase
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 지점명으로 부스 검색 (mock)
 */
@UseCase
class SearchPhotoBoothsByKeywordUseCase {

    fun execute(query: SearchQuery.SearchPhotoBoothsByKeyword): SearchResult.SearchPhotoBooths {
        val matched: List<SearchResult.GetPhotoBooths.Item> = SearchAssembler.toItems(
            booths = SearchMockData.searchPhotoBooths(query.keyword),
            userLocation = query.userLocation,
        )

        val page: Page<SearchResult.GetPhotoBooths.Item> = query.pagination.let {
            it.slice(matched.drop(it.offset).take(it.limit))
        }

        return SearchResult.SearchPhotoBooths(
            items = page.items,
            hasNext = page.hasNext,
            totalCount = matched.size.toLong(),
        )
    }
}
