package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : SearchPhotoBoothsUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:28
 * description    : 검색어에 맞는 부스 목록 조회 (mock)
 */
@UseCase
class SearchPhotoBoothsUseCase {

    /** mock: query 와 무관하게 항상 같은 목록을 내려준다. */
    @Suppress("UNUSED_PARAMETER")
    fun execute(query: SearchQuery.GetPhotoBooths): SearchResult.GetPhotoBooths {
        val items: List<SearchResult.GetPhotoBooths.Item> = SearchMockData.photoBooths.map {
            SearchResult.GetPhotoBooths.Item(
                id = it.id,
                brandName = it.brand.name,
                brandCode = it.brand.code,
                branchName = it.branchName,
                address = it.address,
                latitude = it.latitude,
                longitude = it.longitude,
                distance = it.distance,
                favorite = it.favorite,
            )
        }

        return SearchResult.GetPhotoBooths(items = items)
    }
}
