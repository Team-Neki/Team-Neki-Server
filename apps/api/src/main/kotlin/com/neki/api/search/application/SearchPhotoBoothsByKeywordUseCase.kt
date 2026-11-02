package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.service.PhotoBoothSearchService
import com.neki.domain.search.service.qu.QueryUnderstandingService

/**
 * fileName       : SearchPhotoBoothsByKeywordUseCase
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 부스 검색. 부스 목록·필터 API 와 같은 검색 색인(_read)을 조회한다.
 */
@UseCase
class SearchPhotoBoothsByKeywordUseCase(
    private val photoBoothSearchService: PhotoBoothSearchService,
    private val queryUnderstandingService: QueryUnderstandingService,
    private val visibleBrandFilter: VisibleBrandFilter,
) {

    fun execute(query: SearchQuery.SearchPhotoBoothsByKeyword): SearchResult.Completion {
        // 짧은 검색어는 결과가 비므로 노출 브랜드 ID 조회도 건너뛴다
        val visibleBrandIds: List<Long> = if (query.isTooShort) emptyList() else visibleBrandFilter.allVisibleIds()
        val booths: PageWithTotalCount<PhotoBoothSearch> = photoBoothSearchService.searchByKeyword(
            query,
            visibleBrandIds,
        )

        // 검색어에 적힌 브랜드는 부스 목록 요청의 브랜드 필터로 내려준다
        val visibleBrandIdSet: Set<Long> = visibleBrandIds.toSet()
        val brandIds: List<Long> =
            queryUnderstandingService.recognizeBrandIds(query.keyword).filter { it in visibleBrandIdSet }

        return SearchAssembler.toPhotoBoothCompletion(booths, query.userLocation, brandIds)
    }
}
