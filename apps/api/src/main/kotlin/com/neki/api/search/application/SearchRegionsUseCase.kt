package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.service.RegionSearchService

/**
 * fileName       : SearchRegionsUseCase
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지역 검색
 */
@UseCase
class SearchRegionsUseCase(private val regionSearchService: RegionSearchService) {

    fun execute(query: SearchQuery.SearchRegions): SearchResult.Completion {
        val regions: PageWithTotalCount<LegalDong> = regionSearchService.search(query)

        return SearchAssembler.toRegionCompletion(regions)
    }
}
