package com.neki.domain.search

import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : SubwayStationTest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 역 keyword 형식(`역명역 노선명`)을 만들고 되돌린다
 */
class SubwayStationTest :
    FunSpec({

        val point = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(127.0276, 37.4979))

        test("keyword 는 `역명역 노선명` 이고 parseKeyword 가 되돌린다") {
            val station = SubwayStation(SubwayStationId("강남", "2호선"), point)

            station.nameWithSuffix shouldBe "강남역"
            station.keyword shouldBe "강남역 2호선"
            SubwayStation.parseKeyword(station.keyword) shouldBe station.id
        }

        test("역명과 노선명에 공백이 있어도 처음 나오는 `역 ` 에서 나눈다") {
            SubwayStation.parseKeyword("북구청 (대구iM뱅크파크)역 대구 3호선") shouldBe
                SubwayStationId("북구청 (대구iM뱅크파크)", "대구 3호선")
        }

        test("형식이 아니면 null") {
            SubwayStation.parseKeyword("서울특별시 강남구").shouldBeNull()
            SubwayStation.parseKeyword("강남역").shouldBeNull()
            SubwayStation.parseKeyword("역 2호선").shouldBeNull()
            SubwayStation.parseKeyword("강남역 ").shouldBeNull()
        }
    })
