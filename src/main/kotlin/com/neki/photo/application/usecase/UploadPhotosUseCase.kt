package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.command.UploadPhotoCommand
import com.neki.photo.application.contract.MediaAvailability
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.PhotoImage

/**
 * fileName       : UploadPhotosUseCase
 * author         : koo
 * date           : 2026. 1. 20.
 * description    : 다중 사진 업로드 UseCase (최대 10장)
 */
@UseCase
class UploadPhotosUseCase(
    private val mediaClient: MediaClientPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
    private val folderRepository: FolderRepositoryPort,
    private val favoriteImageRepository: FavoriteImageRepositoryPort,

    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UploadPhotoCommand) {
        validateNoDuplicateMediaIds(command.uploads)
        validateFolderOwnership(command.userId, command.folderId)

        val newUploads: List<UploadPhotoCommand.UploadItem> = filterNewUploads(command.uploads)
        if (newUploads.isEmpty()) return

        val newMediaIds: List<Long> = newUploads.map { it.mediaId }

        val availabilities: Map<Long, MediaAvailability> = mediaClient.verifyMediasUploaded(
            ownerId = command.userId,
            mediaIds = newMediaIds,
        )
        rollbackIfFailed(command.userId, availabilities)

        val photos = newUploads.map { upload ->
            PhotoImage(
                userId = command.userId,
                mediaId = upload.mediaId,
                folderId = command.folderId,
                memo = upload.memo,
                uploadMethod = upload.uploadMethod,
                capturedAt = upload.capturedAt,
            )
        }

        try {
            transactionRunner.run {
                val savedPhotos: List<PhotoImage> = photoImageRepository.saveAll(photos)
                val savedPhotoIds: List<Long> = savedPhotos.map { it.id!! }
                if (command.folderId != null) {
                    photoImageFolderRepository.saveAll(savedPhotoIds, command.folderId)
                }
                if (command.favorite) {
                    favoriteImageRepository.addAll(command.userId, savedPhotoIds)
                }
            }
        } catch (e: BusinessException) {
            if (e.resultCode == ResultCode.ALREADY_REQUEST) return
            mediaClient.rollbackMediasUploaded(command.userId, newMediaIds)
            throw e
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(command.userId, newMediaIds)
            throw e
        }
    }

    private fun filterNewUploads(uploads: List<UploadPhotoCommand.UploadItem>): List<UploadPhotoCommand.UploadItem> {
        val mediaIds: List<Long> = uploads.map { it.mediaId }
        val existingMediaIds: Set<Long> = photoImageRepository.getRegisteredMediaIds(mediaIds)
        return uploads.filter { it.mediaId !in existingMediaIds }
    }

    private fun validateNoDuplicateMediaIds(uploads: List<UploadPhotoCommand.UploadItem>) {
        val mediaIds: List<Long> = uploads.map { it.mediaId }
        val duplicates: Set<Long> = mediaIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    private fun validateFolderOwnership(userId: Long, folderId: Long?) {
        if (folderId != null) {
            folderRepository.getOwnedFolder(userId, folderId)
                ?: throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    private fun rollbackIfFailed(userId: Long, availabilities: Map<Long, MediaAvailability>) {
        val unavailableMediaIds: Set<Long> = availabilities
            .filter { it.value != MediaAvailability.AVAILABLE }
            .keys

        if (unavailableMediaIds.isNotEmpty()) {
            val successfulMediaIds: List<Long> = availabilities
                .filter { it.value == MediaAvailability.AVAILABLE }
                .keys
                .toList()

            if (successfulMediaIds.isNotEmpty()) {
                mediaClient.rollbackMediasUploaded(userId, successfulMediaIds)
            }

            throw BusinessException(ResultCode.UPLOAD_FAILED)
        }
    }
}
