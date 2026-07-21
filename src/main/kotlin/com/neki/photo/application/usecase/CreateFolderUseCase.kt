package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.dto.FolderCommand
import com.neki.photo.application.dto.FolderResult
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.domain.entity.Folder
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
    fun execute(command: FolderCommand.CreateFolder): FolderResult.CreateFolder {
        if (folderRepository.existsOwnedFolderName(command.userId, command.name)) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        val savedFolder: Folder = folderRepository.save(
            Folder(
                userId = command.userId,
                name = command.name,
            ),
        )

        return FolderResult.CreateFolder(savedFolder.id!!)
    }
}
