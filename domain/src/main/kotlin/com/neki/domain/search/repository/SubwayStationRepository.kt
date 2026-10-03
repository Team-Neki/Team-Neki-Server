package com.neki.domain.search.repository

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId

/**
 * fileName       : SubwayStationRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 조회
 */
interface SubwayStationRepository {

    /**
     * 역명이 namePrefix 로 시작하는 역. keyword 에 공백이 있으면 `역명역 노선명` 또는 `역명 노선명` 이 keyword 로 시작해야 한다.
     * 역명, 노선명 순. 다음 페이지 판단을 위해 [Pagination.limit] 만큼 조회한다.
     */
    fun findByKeywordPrefix(keyword: String, namePrefix: String, pagination: Pagination): List<SubwayStation>

    fun countByKeywordPrefix(keyword: String, namePrefix: String): Long

    fun findById(id: SubwayStationId): SubwayStation?
}
