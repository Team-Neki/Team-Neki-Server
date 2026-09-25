package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.service.StationSearchService

/**
 * fileName       : SearchStationsUseCase
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 검색
 */
@UseCase
class SearchStationsUseCase(private val stationSearchService: StationSearchService) {

    fun execute(query: SearchQuery.SearchStations): SearchResult.Completion {
        val stations: PageWithTotalCount<SubwayStation> = stationSearchService.search(query)

        return SearchAssembler.toStationCompletion(stations)
    }
}
