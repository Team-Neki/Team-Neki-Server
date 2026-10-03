package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import java.io.Serializable

/**
 * fileName       : SubwayStation
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : Workflow 가 소유하는 tb_subway_station 의 읽기 전용 매핑. 한 역이 노선마다 따로 있다
 */
@Entity
@Immutable
@Table(name = "tb_subway_station")
class SubwayStation(
    @EmbeddedId
    val id: SubwayStationId,

    /** 승강장 위치. 노선마다 달라 주변 부스도 달라진다. */
    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    val location: Point,
) {
    /** 역명. 저장된 값에는 `역` 이 없다. e.g. `강남` */
    val name: String
        get() = id.name

    val lineName: String
        get() = id.lineName

    /** 역명에 접미사를 붙인 이름. e.g. `강남역` */
    val nameWithSuffix: String
        get() = "$name$SUFFIX"

    /** 자동완성 keyword. e.g. `강남역 2호선`. [parseKeyword] 가 되돌린다 */
    val keyword: String
        get() = "$nameWithSuffix $lineName"

    /** coordinate(경도 x, 위도 y)에서 이 역까지의 거리(m). 검색 API 의 사용자 거리와 같은 haversine */
    fun distanceFrom(coordinate: Coordinate): Int =
        UserLocation(latitude = coordinate.y, longitude = coordinate.x).distanceTo(location.y, location.x)

    companion object {
        const val TABLE = "tb_subway_station"

        /** 저장된 역명에는 없고 화면과 검색어에만 붙는 접미사 */
        const val SUFFIX = "역"

        private const val KEYWORD_SEPARATOR = "$SUFFIX "

        /**
         * 자동완성 keyword 를 역 식별자로 되돌린다. `강남역 2호선` -> (강남, 2호선). 형식이 아니면 null.
         * 역명과 노선명에도 공백이 있어(`부산 도시철도 2호선`) 처음 나오는 `역 ` 에서 나눈다.
         * 역명 안에 `역 ` 이 들어간 역이 있다면 여기서는 찾지 못하고 NER 로 넘어간다.
         */
        fun parseKeyword(keyword: String): SubwayStationId? {
            val index: Int = keyword.indexOf(KEYWORD_SEPARATOR)
            if (index <= 0) return null

            val lineName: String = keyword.substring(index + KEYWORD_SEPARATOR.length)
            if (lineName.isBlank()) return null

            return SubwayStationId(name = keyword.substring(0, index), lineName = lineName)
        }
    }
}

@Embeddable
data class SubwayStationId(
    @Column(name = "name", nullable = false, length = 60)
    val name: String,

    @Column(name = "line_name", nullable = false, length = 40)
    val lineName: String,
) : Serializable
