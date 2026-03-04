package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.command.GetFavoriteSummaryCommand
import com.neki.photo.contract.MediaStorageInfo
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.application.result.GetFavoriteSummaryResult
import com.neki.photo.domain.entity.PhotoImage
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
    private val favoriteImageRepository: FavoriteImageRepositoryPort,
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,

    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetFavoriteSummaryCommand): GetFavoriteSummaryResult {
        val totalCount: Long = transactionRunner.readOnly {
            favoriteImageRepository.countByUserId(command.userId)
        }

        if (totalCount == 0L) {
            return GetFavoriteSummaryResult(storageKey = null, totalCount = 0)
        }

        val latestPhoto: PhotoImage? = transactionRunner.readOnly {
            photoImageRepository.getLatestFavoritePhoto(command.userId)
        }

        if (latestPhoto == null) {
            log.warn("Photo not found but count is {}.", totalCount)
            return GetFavoriteSummaryResult(storageKey = null, totalCount = totalCount)
        }

        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            command.userId,
            listOf(latestPhoto.mediaId),
        )

        val media: MediaStorageInfo? = mediaStorageInfos.firstOrNull()
        if (media == null) {
            log.info("Media not found yet. photoId=${latestPhoto.id}, mediaId=${latestPhoto.mediaId}")
            return GetFavoriteSummaryResult(storageKey = null, totalCount = totalCount)
        }

        return GetFavoriteSummaryResult(storageKey = media.storageKey, totalCount = totalCount)
    }
}
