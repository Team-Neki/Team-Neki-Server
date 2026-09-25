package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.domain.search.infra.persist.jpa.JpaLegalDongRepository
import com.neki.domain.search.infra.persist.jpa.JpaSubwayStationRepository
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import org.junit.jupiter.api.AfterEach
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : SearchE2ETestBase
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지역·역 검색 E2E 테스트를 위한 Base Class
 */
abstract class SearchE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var legalDongRepository: JpaLegalDongRepository

    @Autowired
    protected lateinit var subwayStationRepository: JpaSubwayStationRepository

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    override fun tearDown() {
        legalDongRepository.deleteAllInBatch()
        subwayStationRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun createLegalDong(code: String, level: Int, leafName: String, fullName: String): LegalDong =
        legalDongRepository.save(LegalDong(code = code, level = level, leafName = leafName, fullName = fullName))

    protected fun createSubwayStation(
        name: String,
        lineName: String,
        longitude: Double,
        latitude: Double,
    ): SubwayStation = subwayStationRepository.save(
        SubwayStation(
            id = SubwayStationId(name = name, lineName = lineName),
            location = geometryFactory.createPoint(Coordinate(longitude, latitude)),
        ),
    )
}
