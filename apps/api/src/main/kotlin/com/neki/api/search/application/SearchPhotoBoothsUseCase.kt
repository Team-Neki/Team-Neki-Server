package com.neki.api.search.application

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

        val items: List<SearchResult.GetPhotoBooths.Item> = booths.map {
            SearchResult.GetPhotoBooths.Item(
                id = it.id,
                brandName = it.brand.name,
                brandCode = it.brand.code,
                branchName = it.branchName,
                address = it.address,
                latitude = it.latitude,
                longitude = it.longitude,
                distance = query.userLocation?.distanceTo(it.latitude, it.longitude),
                favorite = it.favorite,
            )
        }

        // 사용자 위치가 있으면 가까운 순, 없으면 브랜드·지점명 순
        val sorted: List<SearchResult.GetPhotoBooths.Item> = if (query.userLocation != null) {
            items.sortedBy { it.distance }
        } else {
            items.sortedWith(compareBy({ it.brandName }, { it.branchName }))
        }

        return SearchResult.GetPhotoBooths(items = sorted)
    }
}
