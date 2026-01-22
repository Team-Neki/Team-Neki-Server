package com.yapp2app.map.infra.geometry

import com.yapp2app.map.application.port.GeometryPort
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service

/**
 * fileName       : GeometryAdapter
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : JTS Geometry 변환 서비스
 */
@Service
class GeometryAdapter : GeometryPort {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * 경도/위도를 Point 객체로 변환
     */
    override fun createPoint(longitude: Double, latitude: Double): Point =
        geometryFactory.createPoint(Coordinate(longitude, latitude))
}
