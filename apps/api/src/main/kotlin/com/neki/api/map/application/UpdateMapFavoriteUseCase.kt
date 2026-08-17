package com.neki.api.map.application

import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.service.MapService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateMapFavoriteUseCase
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@UseCase
class UpdateMapFavoriteUseCase(private val mapService: MapService) {

    @Transactional
    fun execute(command: MapCommand.UpdateMapFavorite) {
        mapService.updateFavoriteMap(command)
    }
}
