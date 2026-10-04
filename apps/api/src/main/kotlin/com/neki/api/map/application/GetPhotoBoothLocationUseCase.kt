package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.api.map.application.dto.PhotoBoothLocationAssembler
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository

/**
 * fileName       : GetPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 17.
 * description    : 포토부스 위치 조회
 */
@UseCase
class GetPhotoBoothLocationUseCase(
    private val photoBoothLocationRepository: PhotoBoothLocationRepository,
    private val favoriteMapRepository: FavoriteMapRepository,
    private val transactionRunner: TransactionRunner,
) {

    /**
     * 다각형 기준으로 포토부스 위치 조회
     */
    fun execute(query: MapQuery.GetPolygonLocation): MapResult.GetPolygonLocation = transactionRunner.readOnly {
        val locations: List<PhotoBoothLocationView> = photoBoothLocationRepository.listPolygonLocations(
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
        val locations: List<PhotoBoothLocationWithDistance> =
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

    /**
     * 수집 원천 키로 포토부스 위치 조회. 관리자가 숨긴 지점과 아직 동기화되지 않은 원천 키는 빠진다
     */
    fun execute(query: MapQuery.GetSourceLocations): MapResult.GetSourceLocations = transactionRunner.readOnly {
        val locations: List<PhotoBoothLocation> = photoBoothLocationRepository.findVisibleBySources(query.sources)

        if (locations.isEmpty()) {
            return@readOnly MapResult.GetSourceLocations(emptyList())
        }

        val favoriteLocationIds: Set<Long> = favoriteMapRepository.findFavoritedLocationIds(
            userId = query.userId,
            locationIds = locations.mapNotNull { it.id },
        )

        PhotoBoothLocationAssembler.toSourceLocations(locations, favoriteLocationIds)
    }
}
