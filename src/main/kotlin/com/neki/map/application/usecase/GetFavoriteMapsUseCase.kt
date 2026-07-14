package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.map.application.command.GetFavoriteMapsCommand
import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.application.result.GetFavoriteMapResult
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFavoriteMapsUseCase
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@UseCase
class GetFavoriteMapsUseCase(private val favoriteMapRepository: FavoriteMapRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(command: GetFavoriteMapsCommand): GetFavoriteMapResult {
        val locations: List<PhotoBoothLocationDto> =
            favoriteMapRepository.findFavoriteLocationsByUserId(command.userId)

        return GetFavoriteMapResult(locations = locations)
    }
}
