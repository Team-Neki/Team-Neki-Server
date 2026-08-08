package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.photo.service.FavoriteService
import com.neki.domain.photo.service.PhotoService

/**
 * fileName       : DeletePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:05
 * description    : 사진 삭제 usecase
 */
@UseCase
class DeletePhotosUseCase(
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: PhotoImageCommand.DeletePhotos) {
        val deletedPhotos: List<PhotoImage> = transactionRunner.run {
            // 즐겨찾기를 먼저 지워야 고아 레코드가 남지 않는다
            favoriteService.removeAll(command)

            photoService.deletePhotos(command)
        }

        // 외부 정리는 트랜잭션 커밋 이후
        mediaClient.deleteMedias(command.userId, deletedPhotos.map { it.mediaId })
    }
}
