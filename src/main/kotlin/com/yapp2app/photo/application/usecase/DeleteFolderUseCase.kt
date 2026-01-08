package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.DeleteFolderCommand
import com.yapp2app.photo.application.command.DeleteFoldersCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : DeleteFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:33
 * description    : 폴더 삭제 usecase
 */
@UseCase
class DeleteFolderUseCase(private val folderRepository: FolderRepositoryPort) {

    @Transactional
    fun execute(command: DeleteFolderCommand) {
        val folder = folderRepository.getOwnedFolder(command.userId, command.folderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        folderRepository.deleteOwnedFolder(command.userId, command.folderId)
    }

    @Transactional
    fun execute(command: DeleteFoldersCommand) {
        val folders = folderRepository.getOwnedFolders(command.userId, command.folderIds)

        if (folders.size != command.folderIds.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        folderRepository.deleteOwnedFolders(command.userId, command.folderIds)
    }
}
