package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.command.DeletePhotosCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.entity.PhotoImage

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
        val deletedPhotos: List<PhotoImage> = transactionRunner.run {
            favoriteImageRepository.deleteAll(command.userId, command.photoIds)

            photoImageRepository.deleteOwnedPhotos(
                command.userId,
                command.photoIds,
            )
        }

        mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
    }
}
