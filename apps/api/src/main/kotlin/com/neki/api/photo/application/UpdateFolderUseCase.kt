package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.service.FolderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 10:23
 * description    : folder update usecase
 */
@UseCase
class UpdateFolderUseCase(private val folderService: FolderService) {

    @Transactional
    fun execute(command: FolderCommand.UpdateFolder) = folderService.renameFolder(command)
}
