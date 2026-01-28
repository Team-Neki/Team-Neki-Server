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
 * fileName       : BulkUploadPhotoUseCase
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

        val mediaIds = command.uploads.map { it.mediaId }

        // 모든 media가 object storage에 정상적으로 저장되었는지 일괄 확인
        val availabilities = mediaClient.verifyMediasUploaded(
            ownerId = command.userId,
            mediaIds = mediaIds,
        )

        rollbackIfFailed(command.userId, availabilities)

        val photos = command.uploads.map { upload ->
            PhotoImage(
                userId = command.userId,
                mediaId = upload.mediaId,
                folderId = command.folderId,
                memo = upload.memo,
            )
        }

        try {
            transactionRunner.run {
                val savedPhotos = photoImageRepository.saveAll(photos)

                if (command.folderId != null) {
                    updateFolderCover(command.userId, command.folderId, savedPhotos)
                }
            }
        } catch (e: Exception) {
            // 보상 트랜잭션: 모든 media 상태를 INITIATED로 롤백
            mediaClient.rollbackMediasUploaded(command.userId, mediaIds)
            throw e
        }
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

    private fun updateFolderCover(userId: Long, folderId: Long, savedPhotos: List<PhotoImage>) {
        val latestPhoto = savedPhotos.maxByOrNull { it.createdAt!! } ?: return

        folderRepository.updateCoverPhotoIfNewer(
            userId = userId,
            folderId = folderId,
            newCoverPhotoId = latestPhoto.id!!,
        )
    }
}
