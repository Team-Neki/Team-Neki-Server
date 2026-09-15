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
        // 페이지에 들어갈 것만 조립하는 다른 목록 조회와 달리 전체를 조립한 뒤 자른다.
        // distance 정렬이 매핑 결과에 의존해 정렬이 페이징보다 앞서야 하기 때문이고, mock 이라 건수가 적어 괜찮다.
        // 실제 구현은 정렬과 페이징을 쿼리로 내려 페이지 밖 항목을 매핑하지 않아야 한다.
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
