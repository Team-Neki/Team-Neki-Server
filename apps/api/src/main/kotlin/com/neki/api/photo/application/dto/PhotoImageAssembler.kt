package com.neki.api.photo.application.dto

import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.models.MediaMetadatas
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.photo.models.PhotoWithFavorite
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : PhotoImageAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 사진에 media 메타데이터를 붙여 응답 항목으로 조립한다.
 */
object PhotoImageAssembler {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toItems(
        userId: Long,
        photos: List<PhotoWithFavorite>,
        medias: List<MediaMetadata>,
    ): List<PhotoImageResult.GetPhotos.Item> {
        val metadatas = MediaMetadatas(medias)

        return photos.mapNotNull { (photo, isFavorite) ->
            val media: MediaMetadata = metadatas.findOrSkip(userId, photo) ?: return@mapNotNull null

            PhotoImageResult.GetPhotos.Item(
                photoId = photo.id!!,
                storageKey = media.storageKey,
                favorite = isFavorite,
                contentType = media.contentType,
                uploadMethod = photo.uploadMethod,
                width = media.width,
                height = media.height,
                memo = photo.memo,
                createdAt = photo.createdAt!!,
                capturedAt = photo.capturedAt,
            )
        }
    }

    /**
     * 즐겨찾기 목록은 전부 favorite = true 다.
     * memo 는 담지 않는다. 즐겨찾기 목록 응답의 기존 계약이며 그대로 유지한다.
     */
    fun toFavoriteItems(
        userId: Long,
        photos: List<PhotoImage>,
        medias: List<MediaMetadata>,
    ): List<PhotoImageResult.GetPhotos.Item> {
        val metadatas = MediaMetadatas(medias)

        return photos.mapNotNull { photo ->
            val media: MediaMetadata = metadatas.findOrSkip(userId, photo) ?: return@mapNotNull null

            PhotoImageResult.GetPhotos.Item(
                photoId = photo.id!!,
                storageKey = media.storageKey,
                favorite = true,
                contentType = media.contentType,
                uploadMethod = photo.uploadMethod,
                width = media.width,
                height = media.height,
                createdAt = photo.createdAt!!,
                capturedAt = photo.capturedAt,
            )
        }
    }

    /**
     * 아직 media가 저장되지 않은 사진은 제외한다 (eventually consistent).
     */
    private fun MediaMetadatas.findOrSkip(userId: Long, photo: PhotoImage): MediaMetadata? =
        this[photo.mediaId] ?: run {
            log.info("Media not found yet. photoId={}, fileId={}, userId={}", photo.id, photo.mediaId, userId)
            null
        }
}
