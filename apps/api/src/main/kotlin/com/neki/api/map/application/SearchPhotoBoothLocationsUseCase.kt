package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.repository.PhotoBoothLocationRepository

/**
 * fileName       : SearchPhotoBoothLocationsUseCase
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 브랜드명·지점명으로 포토부스 검색. 통합 검색 자동완성이 SearchMapClient 로 호출한다.
 */
@UseCase
class SearchPhotoBoothLocationsUseCase(
    private val photoBoothLocationRepository: PhotoBoothLocationRepository,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: MapQuery.SearchPhotoBooths): MapResult.SearchPhotoBooths = transactionRunner.readOnly {
        val fetched: List<PhotoBoothLocationView> = photoBoothLocationRepository.findByNamePrefix(query)
        val totalCount: Long = photoBoothLocationRepository.countByNamePrefix(query.keyword)

        val page: PageWithTotalCount<PhotoBoothLocationView> = query.pagination.slice(fetched, totalCount)
        MapResult.SearchPhotoBooths(locations = page.items, hasNext = page.hasNext, totalCount = page.totalCount)
    }
}
