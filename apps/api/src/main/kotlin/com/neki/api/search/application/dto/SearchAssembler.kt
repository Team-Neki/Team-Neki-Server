package com.neki.api.search.application.dto

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.SubwayStation

/**
 * fileName       : SearchAssembler
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 검색 결과를 자동완성 응답으로 조립한다.
 */
object SearchAssembler {

    /** 같은 이름을 구분할 수 있게 `서울특별시 강남구` 처럼 전체 경로를 내려준다. */
    fun toRegionCompletion(regions: PageWithTotalCount<LegalDong>): SearchResult.Completion = SearchResult.Completion(
        keywords = regions.items.map { it.fullName },
        hasNext = regions.hasNext,
        totalCount = regions.totalCount,
    )

    /** 저장된 역명에는 `역` 이 없다. 화면에 보일 `강남역 2호선` 형태로 맞춰 내려준다. */
    fun toStationCompletion(stations: PageWithTotalCount<SubwayStation>): SearchResult.Completion =
        SearchResult.Completion(
            keywords = stations.items.map { "${it.name}역 ${it.lineName}" },
            hasNext = stations.hasNext,
            totalCount = stations.totalCount,
        )
}
