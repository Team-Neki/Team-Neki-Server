package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.DeletePhotosCommand
import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort

/**
 * fileName       : DeletePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:05
 * description    : 사진 삭제 usecase
 */
@UseCase
class DeletePhotosUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val mediaClient: MediaClientPort,

    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: DeletePhotosCommand) {
        val deletedPhotos = transactionRunner.run {
            favoriteImageRepository.deleteAll(command.userId, command.photoIds)

            photoImageRepository.deleteOwnedPhotos(
                command.userId,
                command.photoIds,
            )
        }

        mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
    }
}
