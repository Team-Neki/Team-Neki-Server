package com.yapp2app.photobooth.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.photobooth.application.command.DeleteFolderCommand
import com.yapp2app.photobooth.application.command.DeleteFoldersCommand
import com.yapp2app.photobooth.application.port.FolderRepositoryPort
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
        val folder = folderRepository.findById(command.folderId)
            ?: throw RuntimeException()

        folder.detachPhotos()

        folderRepository.deleteById(command.folderId)
    }

    @Transactional
    fun execute(command: DeleteFoldersCommand) {
        val folders = folderRepository.findAllByIdIn(command.userId, command.folderIds)

        folders.forEach { it.detachPhotos() }

        folderRepository.deleteAllById(command.folderIds)
    }
}
