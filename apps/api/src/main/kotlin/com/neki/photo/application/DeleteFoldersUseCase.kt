package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.MediaClient
import com.neki.photo.dto.FolderCommand
import com.neki.photo.models.PhotoImage
import com.neki.photo.service.FavoriteService
import com.neki.photo.service.FolderService
import com.neki.photo.service.PhotoService

/**
 * fileName       : DeleteFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:33
 * description    : 폴더 삭제 usecase
 */
@UseCase
class DeleteFoldersUseCase(
    private val folderService: FolderService,
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: FolderCommand.DeleteFolders) {
        // 폴더 삭제로 중간 테이블이 정리되기 전에 대상 사진을 먼저 확보한다
        val photoIdsToDelete: List<Long> = transactionRunner.readOnly {
            folderService.getPhotoIdsToDelete(command)
        }

        val deletedPhotos: List<PhotoImage> = transactionRunner.run {
            // 즐겨찾기 -> 폴더 -> 사진 순으로 지워야 고아 레코드가 남지 않는다
            if (photoIdsToDelete.isNotEmpty()) {
                favoriteService.removeAll(command, photoIdsToDelete)
            }

            // 폴더 삭제 시 중간 테이블은 ON DELETE CASCADE로 함께 정리된다
            folderService.deleteOwnedFolders(command)

            if (photoIdsToDelete.isEmpty()) {
                emptyList()
            } else {
                photoService.deleteOwnedPhotos(command, photoIdsToDelete)
            }
        }

        // 외부 정리는 트랜잭션 커밋 이후
        if (deletedPhotos.isNotEmpty()) {
            mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
        }
    }
}
