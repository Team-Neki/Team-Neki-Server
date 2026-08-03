package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.photo.application.dto.FolderResult
import com.neki.photo.dto.FolderCommand
import com.neki.photo.models.Folder
import com.neki.photo.service.FolderService
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
