package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.command.UpdateMapFavoriteCommand
import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateMapFavoriteUseCase
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@UseCase
class UpdateMapFavoriteUseCase(
    private val photoBoothLocationRepository: PhotoBoothLocationRepositoryPort,
    private val favoriteMapRepository: FavoriteMapRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdateMapFavoriteCommand) {
        val locationExists: Boolean = photoBoothLocationRepository.existsById(command.locationId)

        if (!locationExists) throw BusinessException(ResultCode.NOT_FOUND)

        if (command.favorite) {
            favoriteMapRepository.add(command.userId, command.locationId)
        } else {
            favoriteMapRepository.delete(command.userId, command.locationId)
        }
    }
}
