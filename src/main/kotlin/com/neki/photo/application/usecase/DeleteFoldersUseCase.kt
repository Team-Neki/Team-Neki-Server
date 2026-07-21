package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.dto.FolderCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.PhotoImage

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
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: FolderCommand.DeleteFolders) {
        // 삭제할 사진 ID 조회 (중간 테이블 기준)
        val photoIdsToDelete: List<Long> = if (command.deletePhotos) {
            transactionRunner.readOnly {
                photoImageFolderRepository.getPhotoImageIdsByFolderIds(command.folderIds)
            }
        } else {
            emptyList()
        }

        val deletedPhotos: List<PhotoImage> = transactionRunner.run {
            if (command.deletePhotos) {
                // 사진까지 삭제하는 경우 즐겨찾기 먼저 삭제
                if (photoIdsToDelete.isNotEmpty()) {
                    favoriteImageRepository.deleteAll(command.userId, photoIdsToDelete)
                }
            }

            // 폴더 삭제 (ON DELETE CASCADE로 중간 테이블 자동 정리)
            val deletedCount: Int = folderRepository.deleteOwnedFolders(
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
