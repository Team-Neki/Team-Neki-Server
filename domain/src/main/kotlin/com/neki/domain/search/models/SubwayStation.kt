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

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    val location: Point,
) {
    /** coordinate(경도 x, 위도 y)에서 이 역까지의 거리(m). 검색 API 의 사용자 거리와 같은 haversine */
    fun distanceFrom(coordinate: Coordinate): Int =
        UserLocation(latitude = coordinate.y, longitude = coordinate.x).distanceTo(location.y, location.x)

    companion object {
        const val TABLE = "tb_subway_station"
    }
}

@Embeddable
data class SubwayStationId(
    @Column(name = "name", nullable = false, length = 60)
    val name: String,

    @Column(name = "line_name", nullable = false, length = 40)
    val lineName: String,
) : Serializable
