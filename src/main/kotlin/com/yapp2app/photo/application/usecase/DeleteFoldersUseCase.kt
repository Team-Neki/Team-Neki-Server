package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.DeleteFoldersCommand
import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort

/**
 * fileName       : DeleteFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:33
 * description    : 폴더 삭제 usecase
 */
@UseCase
class DeleteFoldersUseCase(
    private val folderRepository: FolderRepositoryPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: DeleteFoldersCommand) {
        // 삭제할 사진 ID 조회
        val photoIdsToDelete = if (command.deletePhotos) {
            transactionRunner.readOnly {
                photoImageRepository.getPhotoIdsByFolderIds(command.userId, command.folderIds)
            }
        } else {
            emptyList()
        }

        val deletedPhotos = transactionRunner.run {
            // 사진까지 삭제하는 경우 즐겨찾기 먼저 삭제
            if (photoIdsToDelete.isNotEmpty()) {
                favoriteImageRepository.deleteAll(command.userId, photoIdsToDelete)
            }

            // 폴더에서 사진 없앰
            photoImageRepository.updatePhotosFolderIdToNull(command.userId, command.folderIds)

            // 폴더 삭제
            val deletedCount = folderRepository.deleteOwnedFolders(
                command.userId,
                command.folderIds,
            )

            if (deletedCount != command.folderIds.size) {
                throw BusinessException(ResultCode.NOT_FOUND)
            }

            // 사진까지 삭제하는 경우 사진 삭제
            if (photoIdsToDelete.isNotEmpty()) {
                photoImageRepository.deleteOwnedPhotos(command.userId, photoIdsToDelete)
            } else {
                emptyList()
            }
        }

        // 미디어 삭제
        if (deletedPhotos.isNotEmpty()) {
            mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
        }
    }
}
