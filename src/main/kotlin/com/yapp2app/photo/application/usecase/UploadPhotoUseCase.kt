package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.UploadPhotoCommand
import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.application.result.UploadPhotoResult
import com.yapp2app.photo.domain.entity.PhotoImage

/**
 * fileName       : UploadPhotoUseCase
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:24
 * description    : photoImage upload usecase
 */
@UseCase
class UploadPhotoUseCase(
    private val mediaClient: MediaClientPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UploadPhotoCommand): UploadPhotoResult {
        // media가 object storage에 정상적으로 저장되었는지 확인
        val availability = mediaClient.verifyMediaUploaded(
            ownerId = command.userId,
            mediaId = command.mediaId,
        )

        // TODO : 실패한 경우 처리, 재시도 or 실패 간주
        // 현재는 전체 usecase 실패로 간주
        if (availability != MediaAvailability.AVAILABLE) {
            throw BusinessException(ResultCode.UPLOAD_FAILED)
        }

        val photo = PhotoImage(
            userId = command.userId,
            mediaId = command.mediaId,
            folderId = command.folderId,
            memo = command.memo,
        )

        try {
            val savedPhoto = transactionRunner.run { photoImageRepository.save(photo) }
            return UploadPhotoResult(savedPhoto.id!!)
        } catch (e: Exception) {
            // 보상 트랜잭션: media 상태를 INITIATED로 롤백
            mediaClient.rollbackMediaUploaded(command.userId, command.mediaId)
            throw e
        }
    }
}
