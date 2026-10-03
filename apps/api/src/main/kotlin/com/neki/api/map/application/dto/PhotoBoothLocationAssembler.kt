package com.neki.api.map.application.dto

import com.neki.domain.map.models.PhotoBoothLocation

/**
 * fileName       : PhotoBoothLocationAssembler
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 포토부스 위치를 다른 도메인에 넘길 결과로 조립한다.
 */
object PhotoBoothLocationAssembler {

    /**
     * 원천 키로 찾은 지점이라 원천 키가 있다. 그래도 없는 지점은 결과에서 뺀다.
     */
    fun toSourceLocations(
        locations: List<PhotoBoothLocation>,
        favoriteLocationIds: Set<Long>,
    ): MapResult.GetSourceLocations = MapResult.GetSourceLocations(
        locations = locations.mapNotNull { location ->
            val locationId: Long = location.id!!
            location.source()?.let {
                MapResult.GetSourceLocations.SourceLocation(locationId, it, locationId in favoriteLocationIds)
            }
        },
    )
}
