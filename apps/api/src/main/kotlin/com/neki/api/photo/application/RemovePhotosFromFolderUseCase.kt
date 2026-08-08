package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.service.FolderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : RemovePhotosFromFolderUseCase
 * author         : claude
 * date           : 2026. 1. 28.
 * description    : 폴더에서 사진 제외 usecase (연관관계만 해제)
 */
@UseCase
class RemovePhotosFromFolderUseCase(private val folderService: FolderService) {

    @Transactional
    fun execute(command: FolderCommand.RemovePhotosFromFolder) = folderService.removePhotos(command)
}
