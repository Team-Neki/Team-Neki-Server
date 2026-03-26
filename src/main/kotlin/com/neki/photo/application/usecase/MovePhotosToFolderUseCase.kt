package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.MovePhotosToFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import org.springframework.transaction.annotation.Transactional

@UseCase
class MovePhotosToFolderUseCase(
    private val folderRepository: FolderRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
) {

    @Transactional
    fun execute(command: MovePhotosToFolderCommand) {
        if (command.sourceFolderId == command.targetFolderId) return

        // source, target 폴더 소유권 확인 (단일 쿼리)
        val ownedFolders = folderRepository.getOwnedFolders(
            command.userId,
            listOf(command.sourceFolderId, command.targetFolderId),
        )
        if (ownedFolders.size != 2) throw BusinessException(ResultCode.NOT_FOUND)

        // source 폴더에서 연관 삭제
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.sourceFolderId)

        // 멱등성 보장: target 폴더에서 기존 레코드 정리
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.targetFolderId)

        // target 폴더에 연관 추가
        photoImageFolderRepository.saveAll(command.photoIds, command.targetFolderId)
    }
}
