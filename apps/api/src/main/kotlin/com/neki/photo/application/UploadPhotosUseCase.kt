package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.client.MediaClient
import com.neki.photo.dto.PhotoImageCommand
import com.neki.photo.models.MediaAvailabilities
import com.neki.photo.models.PhotoImage
import com.neki.photo.service.FavoriteService
import com.neki.photo.service.FolderService
import com.neki.photo.service.PhotoService

/**
 * fileName       : UploadPhotosUseCase
 * author         : koo
 * date           : 2026. 1. 20.
 * description    : 다중 사진 업로드 UseCase (최대 10장)
 */
@UseCase
class UploadPhotosUseCase(
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
    private val folderService: FolderService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: PhotoImageCommand.UploadPhoto) {
        photoService.validateNoDuplicateMediaIds(command)
        folderService.validateFolderOwnership(command)

        val photos: List<PhotoImage> = photoService.createNewPhotos(command)
        if (photos.isEmpty()) return

        val newMediaIds: List<Long> = photos.map { it.mediaId }
        verifyUploadedOrRollback(command.userId, newMediaIds)

        try {
            transactionRunner.run { persistUpload(command, photos) }
        } catch (e: Exception) {
            // 이미 처리된 요청이면 되돌릴 것이 없다
            if (e is BusinessException && e.resultCode == ResultCode.ALREADY_REQUEST) return

            mediaClient.rollbackMediasUploaded(command.userId, newMediaIds)
            throw e
        }
    }

    /**
     * 사진과 함께 요청받은 폴더 소속, 즐겨찾기를 한 트랜잭션에 반영한다.
     */
    private fun persistUpload(command: PhotoImageCommand.UploadPhoto, photos: List<PhotoImage>) {
        val savedPhotoIds: List<Long> = photoService.savePhotos(photos).map { it.id!! }

        folderService.addPhotosToFolder(command, savedPhotoIds)

        if (command.favorite) {
            favoriteService.addAll(command, savedPhotoIds)
        }
    }

    /**
     * media가 하나라도 스토리지에 없으면 사진을 만들지 않고, 이미 올라간 media는 되돌린다.
     */
    private fun verifyUploadedOrRollback(userId: Long, mediaIds: List<Long>) {
        val availabilities = MediaAvailabilities(
            mediaClient.verifyMediasUploaded(ownerId = userId, mediaIds = mediaIds),
        )

        if (!availabilities.hasUnavailable) return

        val uploadedMediaIds: List<Long> = availabilities.availableMediaIds
        if (uploadedMediaIds.isNotEmpty()) {
            mediaClient.rollbackMediasUploaded(userId, uploadedMediaIds)
        }

        throw BusinessException(ResultCode.UPLOAD_FAILED)
    }
}
