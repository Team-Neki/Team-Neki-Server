package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.RemovePhotosFromFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : RemovePhotosFromFolderUseCase
 * author         : claude
 * date           : 2026. 1. 28.
 * description    : 폴더에서 사진 제외 usecase (연관관계만 해제)
 */
@UseCase
class RemovePhotosFromFolderUseCase(
    private val folderRepository: FolderRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
) {

    @Transactional
    fun execute(command: RemovePhotosFromFolderCommand) {
        // 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.folderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 중간 테이블에서 연관 삭제
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.folderId)
    }
}
