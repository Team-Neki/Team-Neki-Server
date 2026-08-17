package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.service.FolderService
import com.neki.domain.photo.service.PhotoService
import org.springframework.transaction.annotation.Transactional

@UseCase
class MovePhotosToFolderUseCase(private val folderService: FolderService, private val photoService: PhotoService) {

    @Transactional
    fun execute(command: FolderCommand.MovePhotosToFolder) {
        folderService.validateSourceFolder(command)
        folderService.validateFoldersOwned(command)
        photoService.validatePhotosOwned(command)

        folderService.movePhotos(command)
    }
}
