package com.neki.domain.search.service

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.repository.SubwayStationRepository
import org.springframework.stereotype.Component

/**
 * fileName       : StationSearchService
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 이름·`역명역 노선명` 접두 검색
 */
@Component
class StationSearchService(private val subwayStationRepository: SubwayStationRepository) {

    fun search(query: SearchQuery.SearchStations): PageWithTotalCount<SubwayStation> {
        val keyword: String = query.keyword
        if (keyword.length < SearchQuery.MIN_COMPLETION_KEYWORD_LENGTH) return query.pagination.slice(emptyList(), 0L)

        val namePrefix: String = keyword.substringBefore(' ').removeStationSuffix()

        val fetched: List<SubwayStation> =
            subwayStationRepository.findByKeywordPrefix(keyword, namePrefix, query.pagination)
        val totalCount: Long = subwayStationRepository.countByKeywordPrefix(keyword, namePrefix)

        return query.pagination.slice(fetched, totalCount)
    }

    /**
     * 저장된 역명에는 `역` 이 없어 `강남역` 은 `강남` 으로 찾는다. 한 글자짜리 `역` 검색까지 지우지는 않는다.
     * `강남역 2호선` 처럼 노선명까지 적었으면 첫 낱말에서 떼어 역명 후보를 좁힌다.
     */
    private fun String.removeStationSuffix(): String =
        if (length > 1 && endsWith(STATION_SUFFIX)) removeSuffix(STATION_SUFFIX) else this

    companion object {
        private const val STATION_SUFFIX = "역"
    }
}
