package com.yapp2app.photobooth.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.photobooth.application.command.CreateFolderCommand
import com.yapp2app.photobooth.application.port.FolderRepositoryPort
import com.yapp2app.photobooth.domain.entity.Folder
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : CreateFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : 폴더 생성 usecase
 */
@UseCase
class CreateFolderUseCase(private val folderRepository: FolderRepositoryPort) {

    @Transactional
    fun execute(command: CreateFolderCommand) {
        val folder = Folder(
            userId = command.userId,
            name = command.name,
        )

        folderRepository.save(folder)
    }
}
