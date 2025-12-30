package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.UpdateFolderCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
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
        val folder = folderRepository.getOwnedFolder(command.userId, command.folderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 변경하려는 이름이 현재와 다르고, 이미 존재하는 경우
        if (folder.name != command.newName &&
            folderRepository.existsOwnedFolderName(command.userId, command.newName)
        ) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        folder.name = command.newName
    }
}
