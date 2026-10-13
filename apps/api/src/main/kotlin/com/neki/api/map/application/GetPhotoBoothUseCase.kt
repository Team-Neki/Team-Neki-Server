package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.api.map.application.dto.PhotoBoothAssembler
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance
import com.neki.domain.map.repository.PhotoBoothRepository
import com.neki.domain.map.service.MapService

/**
 * fileName       : GetPhotoBoothUseCase
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 조회 (지도 V2, 지금 세대 tb_photo_booth_enriched). 부스는 포토부스 키로 가리킨다
 */
@UseCase
class GetPhotoBoothUseCase(
    private val photoBoothRepository: PhotoBoothRepository,
    private val mapService: MapService,
    private val transactionRunner: TransactionRunner,
) {

    /**
     * 다각형 기준으로 포토부스 조회
     */
    fun execute(query: MapQuery.GetPolygonLocation): MapResult.GetPolygonPhotoBooths = transactionRunner.readOnly {
        val photoBooths: List<PhotoBoothView> = photoBoothRepository.listPolygonPhotoBooths(
            coordinates = query.coordinates,
            brandIds = query.brandIds,
        )

        if (photoBooths.isEmpty()) {
            return@readOnly MapResult.GetPolygonPhotoBooths(emptyList(), emptySet())
        }

        val favoriteKeys: Set<PhotoBoothKey> = mapService.getFavoritedPhotoBoothKeys(query.userId)

        MapResult.GetPolygonPhotoBooths(photoBooths, favoriteKeys)
    }

    /**
     * 특정 Point(사용자) 기준으로 포토부스 조회
     */
    fun execute(query: MapQuery.GetPointLocation): MapResult.GetPointPhotoBooths = transactionRunner.readOnly {
        val photoBooths: List<PhotoBoothWithDistance> = photoBoothRepository.listPointPhotoBooths(
            coordinate = query.coordinate,
            radiusInMeters = query.radiusInMeters,
            brandIds = query.brandIds,
        )

        if (photoBooths.isEmpty()) {
            return@readOnly MapResult.GetPointPhotoBooths(emptyList(), emptySet())
        }

        val favoriteKeys: Set<PhotoBoothKey> = mapService.getFavoritedPhotoBoothKeys(query.userId)

        MapResult.GetPointPhotoBooths(photoBooths, favoriteKeys)
    }

    /**
     * 포토부스 키로 지금 세대에 있는 부스와 즐겨찾기 여부 조회. 검색이 색인 행을 지도 부스와 짝지을 때 쓴다
     */
    fun execute(query: MapQuery.GetPhotoBoothsByKeys): MapResult.GetPhotoBoothsByKeys = transactionRunner.readOnly {
        val existingKeys: Set<PhotoBoothKey> = photoBoothRepository.findExistingKeys(query.keys)

        if (existingKeys.isEmpty()) {
            return@readOnly MapResult.GetPhotoBoothsByKeys(emptyList())
        }

        val favoriteKeys: Set<PhotoBoothKey> = mapService.getFavoritedPhotoBoothKeys(query.userId)

        PhotoBoothAssembler.toPhotoBoothsByKeys(existingKeys, favoriteKeys)
    }
}
