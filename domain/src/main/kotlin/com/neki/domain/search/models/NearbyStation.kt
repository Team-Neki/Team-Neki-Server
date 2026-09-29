package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : NearbyStation
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드에서 반경 안에 있는 지하철역. 카드 테이블의 연결 테이블(*_station) 한 행
 */
@Embeddable
class NearbyStation(
    @Column(name = "station_name", nullable = false, length = 60)
    val stationName: String,

    @Column(name = "line_name", nullable = false, length = 40)
    val lineName: String,

    @Column(name = "distance_m", nullable = false)
    val distanceM: Int,
) {
    companion object {
        const val RADIUS_METERS = 1000

        /** coordinate 에서 RADIUS_METERS 안에 있는 역. 경계 포함 */
        fun within(stations: List<SubwayStation>, coordinate: Coordinate): List<NearbyStation> =
            // ponytail: 카드 x 역 전수 비교. 수천 x 수천이면 충분하고, 그 이상이면 PostGIS ST_DWithin 으로 올린다
            stations.mapNotNull { station ->
                val distance: Int = station.distanceFrom(coordinate)
                NearbyStation(station.id.name, station.id.lineName, distance).takeIf { distance <= RADIUS_METERS }
            }
    }
}
