package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.RemovePhotosFromFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
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
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
) {

    @Transactional
    fun execute(command: RemovePhotosFromFolderCommand) {
        // 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.folderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 사진들의 folderId를 NULL로 설정
        photoImageRepository.removePhotosFromFolder(
            command.userId,
            command.folderId,
            command.photoIds,
        )

        // 중간 테이블에서도 연관 삭제 (dual-write)
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.folderId)
    }
}
