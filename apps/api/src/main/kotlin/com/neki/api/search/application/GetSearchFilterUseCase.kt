package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery

/**
 * fileName       : GetSearchFilterUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:29
 * description    : 부스 목록에서 쓸 수 있는 브랜드 필터 조회 (mock)
 */
@UseCase
class GetSearchFilterUseCase {

    fun execute(query: SearchQuery.GetFilter): SearchResult.GetFilter {
        val booths: List<SearchMockData.PhotoBooth> = SearchMockData.findPhotoBooths(query.target, query.brandIds)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 실제 구현은 브랜드 전체 조회와 같이 사용자별 정렬 순서를 따른다. mock 은 브랜드 ID 순.
        val brandFilter: List<SearchResult.GetFilter.BrandFilter> = booths
            .groupingBy { it.brand }
            .eachCount()
            .entries
            .sortedBy { it.key.id }
            .map { (brand, count) ->
                SearchResult.GetFilter.BrandFilter(id = brand.id, name = brand.name, code = brand.code, count = count)
            }

        return SearchResult.GetFilter(brandFilter = brandFilter)
    }
}
