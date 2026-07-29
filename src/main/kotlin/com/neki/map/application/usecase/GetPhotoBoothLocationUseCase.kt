package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.map.application.dto.MapQuery
import com.neki.map.application.dto.MapResult
import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.map.application.port.dto.MapContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 17.
 * description    : 포토부스 위치 조회
 */
@UseCase
class GetPhotoBoothLocationUseCase(
    private val photoBoothLocationRepository: PhotoBoothLocationRepositoryPort,
    private val favoriteMapRepository: FavoriteMapRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 다각형 기준으로 포토부스 위치 조회
     */
    fun execute(query: MapQuery.GetPolygonLocation): MapResult.GetPolygonLocation = transactionRunner.readOnly {
        val locations: List<MapContract.PhotoBoothLocation> = photoBoothLocationRepository.listPolygonLocations(
            coordinates = query.coordinates,
            brandIds = query.brandIds,
        )

        if (locations.isEmpty()) {
            return@readOnly MapResult.GetPolygonLocation(emptyList(), emptySet())
        }

        val favoriteLocationIds: Set<Long> = favoriteMapRepository.findFavoritedLocationIds(
            userId = query.userId,
            locationIds = locations.map { it.id },
        )

        MapResult.GetPolygonLocation(locations, favoriteLocationIds)
    }

    /**
     * 특정 Point(사용자) 기준으로 포토부스 위치 조회
     */
    fun execute(query: MapQuery.GetPointLocation): MapResult.GetPointLocation = transactionRunner.readOnly {
        val locations: List<MapContract.PhotoBoothLocationWithDistance> =
            photoBoothLocationRepository.listPointLocations(
                coordinate = query.coordinate,
                radiusInMeters = query.radiusInMeters,
                brandIds = query.brandIds,
            )

        if (locations.isEmpty()) {
            return@readOnly MapResult.GetPointLocation(emptyList(), emptySet())
        }

        val favoriteLocationIds: Set<Long> = favoriteMapRepository.findFavoritedLocationIds(
            userId = query.userId,
            locationIds = locations.map { it.id },
        )

        MapResult.GetPointLocation(locations, favoriteLocationIds)
    }
}
