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
) {

    fun execute(query: SearchQuery.SearchPhotoBoothsByKeyword): SearchResult.Completion {
        val booths: PageWithTotalCount<PhotoBoothSearch> = photoBoothSearchService.searchByKeyword(query)

        // 검색어에 적힌 브랜드는 부스 목록 요청의 브랜드 필터로 내려준다
        val brandIds: List<Long> = queryUnderstandingService.recognizeBrandIds(query.keyword)

        return SearchAssembler.toPhotoBoothCompletion(booths, query.userLocation, brandIds)
    }
}
