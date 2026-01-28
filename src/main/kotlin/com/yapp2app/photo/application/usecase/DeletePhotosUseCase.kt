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
 * - 삭제되는 사진이 폴더의 coverId인 경우 보정
 */
@UseCase
class DeletePhotosUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
    private val folderRepository: com.yapp2app.photo.application.port.FolderRepositoryPort,
) {

    fun execute(command: DeletePhotosCommand) {
        // 삭제되는 사진들이 포함된 folderId 조회
        val affectedFolderIds = transactionRunner.readOnly {
            photoImageRepository.getAffectedFolderIds(command.userId, command.photoIds)
        }

        val deletedPhotos = transactionRunner.run {
            favoriteImageRepository.deleteAll(command.userId, command.photoIds)

            val photos = photoImageRepository.deleteOwnedPhotos(
                command.userId,
                command.photoIds,
            )

            if (affectedFolderIds.isNotEmpty()) {
                folderRepository.recalculateCoverPhotos(command.userId, affectedFolderIds)
            }

            photos
        }

        mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
    }
}
