package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.client.PhotoBoothClient
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothSummary

/**
 * fileName       : SearchPhotoBoothsByKeywordUseCase
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 부스 검색. 포토부스는 map 도메인에 있어 PhotoBoothClient 로 조회한다.
 */
@UseCase
class SearchPhotoBoothsByKeywordUseCase(private val photoBoothClient: PhotoBoothClient) {

    fun execute(query: SearchQuery.SearchPhotoBoothsByKeyword): SearchResult.Completion {
        // 지역·역 검색과 같이 1자는 조회하지 않는다
        val booths: PageWithTotalCount<PhotoBoothSummary> =
            if (query.keyword.length < SearchQuery.MIN_COMPLETION_KEYWORD_LENGTH) {
                query.pagination.slice(emptyList(), 0L)
            } else {
                photoBoothClient.searchByName(query)
            }

        return SearchAssembler.toPhotoBoothCompletion(booths, query.userLocation)
    }
}
