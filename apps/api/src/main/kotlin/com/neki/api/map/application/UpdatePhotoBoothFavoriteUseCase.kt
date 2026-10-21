package com.neki.api.map.application

import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.service.MapService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdatePhotoBoothFavoriteUseCase
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 즐겨찾기 등록/해제 (지도 V2)
 */
@UseCase
class UpdatePhotoBoothFavoriteUseCase(private val mapService: MapService) {

    @Transactional
    fun execute(command: MapCommand.UpdatePhotoBoothFavorite) {
        mapService.updateFavoritePhotoBooth(command)
    }
}
