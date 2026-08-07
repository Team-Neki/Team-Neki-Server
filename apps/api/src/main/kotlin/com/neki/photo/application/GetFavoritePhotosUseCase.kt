package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.common.domain.vo.Page
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.dto.PhotoImageAssembler
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.client.MediaClient
import com.neki.photo.dto.PhotoImageQuery
import com.neki.photo.models.MediaMetadata
import com.neki.photo.models.PhotoImage
import com.neki.photo.service.PhotoService

/**
 * fileName       : GetFavoritePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 13. 오후 10:30
 * description    :
 */
@UseCase
class GetFavoritePhotosUseCase(
    private val photoService: PhotoService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PhotoImageQuery.GetFavoritePhotos): PhotoImageResult.GetPhotos {
        val page: Page<PhotoImage> = transactionRunner.readOnly {
            photoService.listFavoritePhotos(query)
        }
        val totalCount: Long = photoService.countFavoritePhotos(query)

        if (page.items.isEmpty()) {
            return PhotoImageResult.GetPhotos(emptyList(), hasNext = false, totalCount = totalCount)
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(
            query.userId,
            page.items.map { it.mediaId },
        )

        return PhotoImageResult.GetPhotos(
            photos = PhotoImageAssembler.toFavoriteItems(query.userId, page.items, medias),
            hasNext = page.hasNext,
            totalCount = totalCount,
        )
    }
}
