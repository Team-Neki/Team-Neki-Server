package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.dto.FolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.Folder
import com.neki.photo.domain.entity.PhotoImage
import org.springframework.transaction.annotation.Transactional

@UseCase
class MovePhotosToFolderUseCase(
    private val folderRepository: FolderRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
) {

    @Transactional
    fun execute(command: FolderCommand.MovePhotosToFolder) {
        // source 폴더 소유권 확인
        folderRepository.getOwnedFolder(command.userId, command.sourceFolderId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        // target 폴더 소유권 확인
        val ownedFolders: List<Folder> = folderRepository.getOwnedFolders(command.userId, command.targetFolderIds)

        if (command.targetFolderIds.size != ownedFolders.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        // 사진 소유권 확인
        val ownedPhotos: List<PhotoImage> = photoImageRepository.getOwnedPhotos(command.userId, command.photoIds)

        if (command.photoIds.size != ownedPhotos.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        // source 폴더에서 연관 삭제
        command.sourceFolderId?.let {
            photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, it)
        }

        // 멱등성 보장: target 폴더들에 이미 존재하는 (사진, 폴더) 매핑을 한번에 조회
        val existingPairs: Set<Pair<Long, Long>> =
            photoImageFolderRepository.findByPhotoImageIdsAndFolderIds(command.photoIds, command.targetFolderIds)
                .map { it.photoImageId to it.folderId }
                .toSet()

        val newMappings: List<Pair<Long, Long>> = command.targetFolderIds.flatMap { folderId ->
            command.photoIds
                .filter { photoId -> (photoId to folderId) !in existingPairs }
                .map { photoId -> photoId to folderId }
        }

        if (newMappings.isNotEmpty()) {
            photoImageFolderRepository.saveAll(newMappings)
        }
    }
}
