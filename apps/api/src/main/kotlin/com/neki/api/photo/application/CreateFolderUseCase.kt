package com.neki.api.photo.application

import com.neki.api.photo.application.dto.FolderResult
import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.models.Folder
import com.neki.domain.photo.service.FolderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : CreateFolderUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : 폴더 생성 usecase
 */
@UseCase
class CreateFolderUseCase(private val folderService: FolderService) {

    @Transactional
    fun execute(command: FolderCommand.CreateFolder): FolderResult.CreateFolder {
        val savedFolder: Folder = folderService.createFolder(command)

        return FolderResult.CreateFolder(savedFolder.id!!)
    }
}
