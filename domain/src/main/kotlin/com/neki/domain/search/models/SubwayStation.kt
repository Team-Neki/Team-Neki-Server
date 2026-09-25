package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.locationtech.jts.geom.Point
import java.io.Serializable

/**
 * fileName       : SubwayStation
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역. 한 역이 노선마다 따로 존재한다. 적재한 참조 테이블이라 애플리케이션에서 수정하지 않는다.
 *                  테이블은 데이터 적재 워크플로가 새 테이블을 만든 뒤 이름을 바꿔치기하는 식으로 관리한다.
 *                  이 저장소는 참조만 하므로 Flyway 마이그레이션을 두지 않고, 인덱스도 워크플로 쪽에서 건다.
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
}

@Embeddable
data class SubwayStationId(
    @Column(name = "name", nullable = false, length = 60)
    val name: String,

    @Column(name = "line_name", nullable = false, length = 40)
    val lineName: String,
) : Serializable
