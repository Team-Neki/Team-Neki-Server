package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.UpdatePhotoFavoriteCommand
import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
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

        if (command.favorite) {
            favoriteImageRepository.add(command.userId, command.photoId)
        } else {
            favoriteImageRepository.remove(command.userId, command.photoId)
        }
    }
}
