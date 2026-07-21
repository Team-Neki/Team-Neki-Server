package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.UpdatePhotoFavoriteCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.entity.FavoritePhoto
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdatePhotoFavoriteUseCase
 * author         : koo
 * date           : 2026. 1. 13. 오후 5:14
 * description    :
 */
@UseCase
class UpdatePhotoFavoriteUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdatePhotoFavoriteCommand) {
        val photoExists: Boolean =
            photoImageRepository.existsOwnedPhoto(command.userId, command.photoId)

        if (!photoExists) throw BusinessException(ResultCode.NOT_FOUND)

        val favoritePhoto = FavoritePhoto(command.userId, command.photoId)
        if (command.favorite) {
            favoriteImageRepository.add(favoritePhoto)
        } else {
            favoriteImageRepository.delete(favoritePhoto)
        }
    }
}
