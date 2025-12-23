package com.yapp2app.photobooth.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.photobooth.application.command.UpdateFolderCommand
import com.yapp2app.photobooth.application.port.FolderRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 10:23
 * description    : folder update usecase
 */
@UseCase
class UpdateFolderUseCase(private val folderRepository: FolderRepositoryPort) {

    @Transactional
    fun execute(command: UpdateFolderCommand) {
        val folder = folderRepository.findById(command.folderId)
            ?: throw RuntimeException()

        folder.name = command.name
    }
}
