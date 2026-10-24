package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.service.MapService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFavoritePhotoBoothsUseCase
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 즐겨찾기한 포토부스 조회 (지도 V2)
 */
@UseCase
class GetFavoritePhotoBoothsUseCase(private val mapService: MapService) {

    @Transactional(readOnly = true)
    fun execute(query: MapQuery.GetFavoriteMaps): MapResult.GetFavoritePhotoBooths {
        val photoBooths: List<PhotoBoothView> = mapService.getFavoritePhotoBooths(query)

        return MapResult.GetFavoritePhotoBooths(photoBooths = photoBooths)
    }
}
