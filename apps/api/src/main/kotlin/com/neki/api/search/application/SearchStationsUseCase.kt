package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.client.PhotoBoothClient
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.CompletionKeyword
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.service.StationSearchService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : SearchStationsUseCase
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 검색
 */
@UseCase
class SearchStationsUseCase(
    private val stationSearchService: StationSearchService,
    private val photoBoothClient: PhotoBoothClient,
) {

    @Transactional(readOnly = true)
    fun execute(query: SearchQuery.SearchStations): SearchResult.Completion {
        // 세 탭이 같은 검색어를 받는다. `강남 포토그레이` 의 브랜드 낱말은 역 검색에 쓰지 않는다
        val stations: PageWithTotalCount<SubwayStation> = stationSearchService.search(
            query.copy(keyword = withoutBrandWords(query.keyword)),
        )

        return SearchAssembler.toStationCompletion(stations, query.userLocation)
    }

    /** 낱말이 하나면 브랜드 목록을 조회하지 않는다 (CompletionKeyword.withoutBrandWords 가 그대로 돌려준다) */
    private fun withoutBrandWords(keyword: String): String {
        if (!keyword.contains(' ')) return keyword

        val brandNames: List<String> = photoBoothClient.findBrandNames()
        return CompletionKeyword.withoutBrandWords(keyword, brandNames)
    }
}
