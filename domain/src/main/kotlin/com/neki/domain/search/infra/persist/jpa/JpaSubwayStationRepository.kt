package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaSubwayStationRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : SubwayStation JPA Repository
 */
interface JpaSubwayStationRepository : JpaRepository<SubwayStation, SubwayStationId>
