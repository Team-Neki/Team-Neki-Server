package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.CreateFolderCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.result.CreateFolderResult
import com.yapp2app.photo.domain.entity.Folder
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
    fun execute(command: CreateFolderCommand): CreateFolderResult {
        if (folderRepository.existsOwnedFolderName(command.userId, command.name)) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        val savedFolder = folderRepository.save(
            Folder(
                userId = command.userId,
                name = command.name,
            ),
        )

        return CreateFolderResult(savedFolder.id!!)
    }
}
