package com.neki.api.photo.application

import com.neki.api.photo.application.dto.PhotoImageAssembler
import com.neki.api.photo.application.dto.PhotoImageResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.dto.PhotoImageQuery
import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.models.PhotoWithFavorite
import com.neki.domain.photo.service.PhotoService

/**
 * fileName       : GetPhotosUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 3:29
 * description    : photoImage 목록 조회
 */
@UseCase
class GetPhotosUseCase(private val photoService: PhotoService, private val mediaClient: MediaClient) {

    fun execute(query: PhotoImageQuery.GetPhotos): PhotoImageResult.GetPhotos {
        val page: Page<PhotoWithFavorite> = photoService.listPhotosWithFavorite(query)
        val totalCount: Long = photoService.countPhotos(query)

        if (page.items.isEmpty()) {
            return PhotoImageResult.GetPhotos(emptyList(), hasNext = false, totalCount = totalCount)
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(
            query.userId,
            page.items.map { it.photo.mediaId },
        )

        return PhotoImageResult.GetPhotos(
            photos = PhotoImageAssembler.toItems(query.userId, page.items, medias),
            hasNext = page.hasNext,
            totalCount = totalCount,
        )
    }
}
