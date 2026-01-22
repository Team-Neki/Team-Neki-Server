package com.yapp2app.map.application.port

import org.locationtech.jts.geom.Point

interface GeometryPort {
    fun createPoint(longitude: Double, latitude: Double): Point
}
