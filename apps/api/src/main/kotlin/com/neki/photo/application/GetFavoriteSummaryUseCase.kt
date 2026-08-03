package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.MediaClient
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.dto.PhotoImageQuery
import com.neki.photo.models.MediaMetadata
import com.neki.photo.models.PhotoImage
import com.neki.photo.service.FavoriteService
import com.neki.photo.service.PhotoService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetFavoriteSummaryUseCase
 * author         : claude
 * date           : 2026. 1. 22.
 * description    : 즐겨찾기 사진 요약 정보 조회 UseCase
 */
@UseCase
class GetFavoriteSummaryUseCase(
    private val photoService: PhotoService,
    private val favoriteService: FavoriteService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(query: PhotoImageQuery.GetFavoriteSummary): PhotoImageResult.GetFavoriteSummary {
        val totalCount: Long = transactionRunner.readOnly { favoriteService.count(query) }

        // 즐겨찾기가 없으면 커버 이미지를 찾을 필요가 없다
        if (totalCount == 0L) {
            return PhotoImageResult.GetFavoriteSummary(storageKey = null, totalCount = totalCount)
        }

        return PhotoImageResult.GetFavoriteSummary(
            storageKey = findCoverStorageKey(query, totalCount),
            totalCount = totalCount,
        )
    }

    /**
     * 커버 이미지는 목록 진입 전 미리보기라, 구하지 못해도 요약 자체는 건수와 함께 반환한다.
     */
    private fun findCoverStorageKey(query: PhotoImageQuery.GetFavoriteSummary, totalCount: Long): String? {
        val latestPhoto: PhotoImage = transactionRunner.readOnly { photoService.getLatestFavoritePhoto(query) }
            ?: run {
                log.warn("Photo not found but count is {}.", totalCount)
                return null
            }

        val media: MediaMetadata = mediaClient.getMediaMetadata(query.userId, listOf(latestPhoto.mediaId))
            .firstOrNull()
            ?: run {
                log.info("Media not found yet. photoId={}, mediaId={}", latestPhoto.id, latestPhoto.mediaId)
                return null
            }

        return media.storageKey
    }
}
