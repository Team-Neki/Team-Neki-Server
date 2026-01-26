package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.DeleteFoldersCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import org.springframework.transaction.annotation.Transactional

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
) {

    @Transactional
    fun execute(command: DeleteFoldersCommand) {
        photoImageRepository.updatePhotosFolderIdToNull(command.userId, command.folderIds)

        val deletedCount = folderRepository.deleteOwnedFolders(
            command.userId,
            command.folderIds,
        )

        if (deletedCount != command.folderIds.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }
}
