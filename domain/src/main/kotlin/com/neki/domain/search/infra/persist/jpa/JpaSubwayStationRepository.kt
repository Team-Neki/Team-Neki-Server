package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaSubwayStationRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : SubwayStation JPA Repository (읽기 전용)
 */
interface JpaSubwayStationRepository : JpaRepository<SubwayStation, SubwayStationId>
