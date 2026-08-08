package com.neki.domain.photo.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.dto.FolderQuery
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.models.Folder
import com.neki.domain.photo.models.FolderStats
import com.neki.domain.photo.repository.FolderRepository
import com.neki.domain.photo.repository.PhotoImageFolderRepository
import org.springframework.stereotype.Component

/**
 * fileName       : FolderService
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 폴더 도메인 서비스. 폴더 애그리거트의 불변식과 사진-폴더 매핑만 다룬다.
 */
@Component
class FolderService(
    private val folderRepository: FolderRepository,
    private val photoImageFolderRepository: PhotoImageFolderRepository,
) {

    /**
     * 같은 사용자 안에서 폴더 이름은 중복될 수 없다.
     */
    fun createFolder(command: FolderCommand.CreateFolder): Folder {
        if (folderRepository.existsOwnedFolderName(command.userId, command.name)) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        return folderRepository.save(Folder(userId = command.userId, name = command.name))
    }

    fun renameFolder(command: FolderCommand.UpdateFolder) {
        val folder: Folder = getOwnedFolder(command.userId, command.folderId)

        // 변경하려는 이름이 현재와 다르고, 이미 존재하는 경우
        if (folder.name != command.newName &&
            folderRepository.existsOwnedFolderName(command.userId, command.newName)
        ) {
            throw BusinessException(ResultCode.CONFLICT_FOLDER)
        }

        folder.rename(command.newName)
    }

    fun listFoldersWithStats(query: FolderQuery.GetFolders): List<FolderStats> =
        folderRepository.listOwnedFoldersWithStats(query.userId, query.limit)

    /**
     * 업로드 대상 폴더를 지정했다면 내 폴더인지 확인한다.
     */
    fun validateFolderOwnership(command: PhotoImageCommand.UploadPhoto) {
        command.folderId?.let { getOwnedFolder(command.userId, it) }
    }

    fun validateSourceFolder(command: FolderCommand.MovePhotosToFolder) {
        getOwnedFolder(command.userId, command.sourceFolderId)
    }

    /**
     * 대상 폴더가 모두 해당 사용자 소유인지 확인한다.
     */
    fun validateFoldersOwned(command: FolderCommand.PhotosToTargetFolders) {
        val ownedFolders: List<Folder> = folderRepository.getOwnedFolders(command.userId, command.targetFolderIds)

        if (command.targetFolderIds.size != ownedFolders.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    /**
     * 소유하지 않은 폴더가 섞여 있으면 삭제 건수가 요청 건수와 달라지므로 예외로 처리한다.
     */
    fun deleteOwnedFolders(command: FolderCommand.DeleteFolders) {
        val deletedCount: Int = folderRepository.deleteOwnedFolders(command.userId, command.folderIds)

        if (deletedCount != command.folderIds.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    /**
     * 사진까지 함께 지우는 요청일 때만 대상 사진을 모은다.
     */
    fun getPhotoIdsToDelete(command: FolderCommand.DeleteFolders): List<Long> = if (command.deletePhotos) {
        photoImageFolderRepository.getPhotoImageIdsByFolderIds(command.folderIds)
    } else {
        emptyList()
    }

    /**
     * 폴더 소유를 확인한 뒤 연관만 해제한다 (사진 자체는 남는다).
     */
    fun removePhotos(command: FolderCommand.RemovePhotosFromFolder) {
        getOwnedFolder(command.userId, command.folderId)

        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.folderId)
    }

    /**
     * 원본 폴더에서 떼어내고 대상 폴더들에 담는다. 둘 다 폴더 애그리거트 안의 일이다.
     */
    fun movePhotos(command: FolderCommand.MovePhotosToFolder) {
        photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(command.photoIds, command.sourceFolderId)

        addPhotosToFolders(command)
    }

    /**
     * 이미 담겨 있는 (사진, 폴더) 조합은 건너뛰어 반복 요청에도 결과가 같도록 한다.
     */
    fun addPhotosToFolders(command: FolderCommand.PhotosToTargetFolders) {
        val existingPairs: Set<Pair<Long, Long>> =
            photoImageFolderRepository
                .findByPhotoImageIdsAndFolderIds(command.photoIds, command.targetFolderIds)
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

    /**
     * 오케스트레이션 중에 정해지는 사진 목록을 담는다 (command에 담기지 않는 값).
     */
    fun addPhotosToFolder(command: PhotoImageCommand.UploadPhoto, photoIds: List<Long>) {
        command.folderId?.let { photoImageFolderRepository.saveAll(photoIds, it) }
    }

    private fun getOwnedFolder(userId: Long, folderId: Long): Folder = folderRepository.getOwnedFolder(userId, folderId)
        ?: throw BusinessException(ResultCode.NOT_FOUND)
}
