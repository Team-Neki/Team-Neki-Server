package com.neki.domain.search.repository

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.SubwayStation

/**
 * fileName       : SubwayStationRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 조회
 */
interface SubwayStationRepository {

    /**
     * 역명이 prefix 로 시작하는 역. 역명, 노선명 순.
     * 다음 페이지 판단을 위해 [Pagination.limit] 만큼 조회한다.
     */
    fun findByNamePrefix(prefix: String, pagination: Pagination): List<SubwayStation>

    fun countByNamePrefix(prefix: String): Long
}
