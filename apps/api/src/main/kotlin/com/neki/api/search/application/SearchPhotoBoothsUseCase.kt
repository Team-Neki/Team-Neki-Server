package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : SearchPhotoBoothsUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:28
 * description    : 고른 지역·역의 부스 목록 조회 (mock)
 */
@UseCase
class SearchPhotoBoothsUseCase {

    fun execute(query: SearchQuery.GetPhotoBooths): SearchResult.GetPhotoBooths {
        val booths: List<SearchMockData.PhotoBooth> = SearchMockData.findPhotoBooths(query.target, query.brandIds)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        return SearchResult.GetPhotoBooths(items = SearchAssembler.toItems(booths, query.userLocation))
    }
}
