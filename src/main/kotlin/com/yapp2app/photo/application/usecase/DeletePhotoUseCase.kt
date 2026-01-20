package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.DeletePhotoCommand
import com.yapp2app.photo.application.command.DeletePhotosCommand
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort

/**
 * fileName       : DeletePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:05
 * description    : 사진 삭제 usecase
 */
@UseCase
class DeletePhotoUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: DeletePhotoCommand) {
        // 사진 삭제
        val photo = transactionRunner.run {
            photoImageRepository.deleteOwnedPhoto(
                command.userId,
                command.photoId,
            )
        } ?: throw BusinessException(ResultCode.NOT_FOUND)

        // 미디어 삭제 요청
        mediaClient.deleteMedia(command.userId, photo.mediaId)
    }

    fun execute(command: DeletePhotosCommand) {
        val photos = transactionRunner.run {
            photoImageRepository.deleteOwnedPhotos(
                command.userId,
                command.photoIds,
            )
        }

        val mediaIds = photos.map { it.mediaId }.toList()

        mediaClient.deleteMedias(command.userId, mediaIds)
    }
}
