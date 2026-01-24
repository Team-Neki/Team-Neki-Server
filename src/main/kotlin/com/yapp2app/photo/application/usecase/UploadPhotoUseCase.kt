package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.UploadPhotoCommand
import com.yapp2app.photo.application.contract.MediaAvailability
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
class UploadPhotoUseCase(
    private val mediaClient: MediaClientPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UploadPhotoCommand) {
        validateNoDuplicateMediaIds(command.uploads)

        val mediaIds = command.uploads.map { it.mediaId }

        // 모든 media가 object storage에 정상적으로 저장되었는지 일괄 확인
        val availabilities = mediaClient.verifyMediasUploaded(
            ownerId = command.userId,
            mediaIds = mediaIds,
        )

        // 업로드 실패한 media가 있는지 확인
        val unavailableMediaIds = availabilities
            .filter { it.value != MediaAvailability.AVAILABLE }
            .keys

        if (unavailableMediaIds.isNotEmpty()) {
            // 성공한 media들도 롤백 (상태를 INITIATED로 되돌림)
            val successfulMediaIds = availabilities
                .filter { it.value == MediaAvailability.AVAILABLE }
                .keys
                .toList()

            if (successfulMediaIds.isNotEmpty()) {
                mediaClient.rollbackMediasUploaded(command.userId, successfulMediaIds)
            }

            throw BusinessException(ResultCode.UPLOAD_FAILED)
        }

        // PhotoImage 엔티티 생성
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
                photoImageRepository.saveAll(photos)
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
}
