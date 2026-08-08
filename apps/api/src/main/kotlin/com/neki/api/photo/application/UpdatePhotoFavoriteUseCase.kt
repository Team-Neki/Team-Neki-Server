package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.service.FavoriteService
import com.neki.domain.photo.service.PhotoService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdatePhotoFavoriteUseCase
 * author         : koo
 * date           : 2026. 1. 13. 오후 5:14
 * description    :
 */
@UseCase
class UpdatePhotoFavoriteUseCase(
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
) {

    @Transactional
    fun execute(command: PhotoImageCommand.UpdatePhotoFavorite) {
        photoService.validatePhotoOwned(command)

        if (command.favorite) {
            favoriteService.add(command)
        } else {
            favoriteService.remove(command)
        }
    }
}
