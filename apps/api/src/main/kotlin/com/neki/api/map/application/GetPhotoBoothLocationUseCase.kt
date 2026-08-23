package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.service.MapService

/**
 * fileName       : GetPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 17.
 * description    : 포토부스 위치 조회
 */
@UseCase
class GetPhotoBoothLocationUseCase(
    private val mapService: MapService,
    private val transactionRunner: TransactionRunner,
) {

    /**
     * 다각형 기준으로 포토부스 위치 조회
     */
    fun execute(query: MapQuery.GetPolygonLocation): MapResult.GetPolygonLocation = transactionRunner.readOnly {
        val locations: List<PhotoBoothLocationView> = mapService.getPolygonLocations(query)

        if (locations.isEmpty()) {
            return@readOnly MapResult.GetPolygonLocation(emptyList(), emptySet())
        }

        val favoriteLocationIds: Set<Long> = mapService.findFavoritedLocationIds(
            userId = query.userId,
            locationIds = locations.map { it.id },
        )

        MapResult.GetPolygonLocation(locations, favoriteLocationIds)
    }

    /**
     * 특정 Point(사용자) 기준으로 포토부스 위치 조회
     */
    fun execute(query: MapQuery.GetPointLocation): MapResult.GetPointLocation = transactionRunner.readOnly {
        val locations: List<PhotoBoothLocationWithDistance> = mapService.getPointLocations(query)

        if (locations.isEmpty()) {
            return@readOnly MapResult.GetPointLocation(emptyList(), emptySet())
        }

        val favoriteLocationIds: Set<Long> = mapService.findFavoritedLocationIds(
            userId = query.userId,
            locationIds = locations.map { it.id },
        )

        MapResult.GetPointLocation(locations, favoriteLocationIds)
    }
}
