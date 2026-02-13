package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.UploadPhotoCommand
import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.domain.entity.PhotoImage

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
    private val folderRepository: FolderRepositoryPort,

    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UploadPhotoCommand) {
        validateNoDuplicateMediaIds(command.uploads)
        validateFolderOwnership(command.userId, command.folderId)

        val newUploads = filterNewUploads(command.uploads)
        if (newUploads.isEmpty()) return

        val newMediaIds = newUploads.map { it.mediaId }

        val availabilities = mediaClient.verifyMediasUploaded(
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
            )
        }

        try {
            transactionRunner.run {
                photoImageRepository.saveAll(photos)
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
        val mediaIds = uploads.map { it.mediaId }
        val existingMediaIds = photoImageRepository.getRegisteredMediaIds(mediaIds)
        return uploads.filter { it.mediaId !in existingMediaIds }
    }

    private fun validateNoDuplicateMediaIds(uploads: List<UploadPhotoCommand.UploadItem>) {
        val mediaIds = uploads.map { it.mediaId }
        val duplicates = mediaIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

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
        val unavailableMediaIds = availabilities
            .filter { it.value != MediaAvailability.AVAILABLE }
            .keys

        if (unavailableMediaIds.isNotEmpty()) {
            val successfulMediaIds = availabilities
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
