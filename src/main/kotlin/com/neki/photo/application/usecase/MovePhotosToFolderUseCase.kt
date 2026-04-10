package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.MovePhotosToFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.PhotoImageFolder
import org.springframework.transaction.annotation.Transactional

@UseCase
class MovePhotosToFolderUseCase(
    private val folderRepository: FolderRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
) {

    @Transactional
    fun execute(command: MovePhotosToFolderCommand) {
        if (command.sourceFolderId == command.targetFolderId) return

        // source 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.sourceFolderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // target 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.targetFolderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 사진 소유권 확인
        command.photoIds.forEach { photoId ->
            if (!photoImageRepository.existsOwnedPhoto(command.userId, photoId)) {
                throw BusinessException(ResultCode.NOT_FOUND)
            }
        }

        // source 폴더에서 연관 삭제
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.sourceFolderId)

        // 멱등성 보장: target 폴더에 이미 존재하는 사진 ID 제외 후 추가
        val existing: List<PhotoImageFolder> =
            photoImageFolderRepository.findByPhotoImageIdsAndFolderId(command.photoIds, command.targetFolderId)
        val existingPhotoIds: Set<Long> = existing.map { it.photoImageId }.toSet()
        val newPhotoIds: List<Long> = command.photoIds.filter { it !in existingPhotoIds }

        if (newPhotoIds.isNotEmpty()) {
            photoImageFolderRepository.saveAll(newPhotoIds, command.targetFolderId)
        }
    }
}
