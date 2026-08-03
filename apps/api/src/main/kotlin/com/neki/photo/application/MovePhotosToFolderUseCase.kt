package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.photo.dto.FolderCommand
import com.neki.photo.service.FolderService
import com.neki.photo.service.PhotoService
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
