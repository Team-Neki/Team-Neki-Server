package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.photo.application.command.RemovePhotosFromFolderCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
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
) {

    @Transactional
    fun execute(command: RemovePhotosFromFolderCommand) {
        // 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.folderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 사진들의 folderId를 NULL로 설정
        val removedCount = photoImageRepository.removePhotosFromFolder(
            command.userId,
            command.folderId,
            command.photoIds,
        )

        // 커버 재계산 (사진이 제외된 경우에만)
        if (removedCount > 0) {
            folderRepository.recalculateCoverPhotos(command.userId, listOf(command.folderId))
        }
    }
}
