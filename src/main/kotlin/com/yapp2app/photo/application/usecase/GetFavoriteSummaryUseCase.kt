package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.properties.AppProperties
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.GetFavoriteSummaryCommand
import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.application.result.GetFavoriteSummaryResult
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
    private val appProperties: AppProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetFavoriteSummaryCommand): GetFavoriteSummaryResult {
        val totalCount = transactionRunner.readOnly {
            favoriteImageRepository.countByUserId(command.userId)
        }

        if (totalCount == 0L) {
            return GetFavoriteSummaryResult(latestImageUrl = null, totalCount = 0)
        }

        val latestPhoto = transactionRunner.readOnly {
            photoImageRepository.getLatestOwnedPhoto(command.userId)
        }

        if (latestPhoto == null) {
            log.warn("Photo not found but count is {}.", totalCount)
            return GetFavoriteSummaryResult(latestImageUrl = null, totalCount = totalCount)
        }

        val mediaStorageInfos = mediaClient.getMediaStorageInfos(
            command.userId,
            listOf(latestPhoto.mediaId),
        )

        val media = mediaStorageInfos.firstOrNull()
        if (media == null) {
            log.info(
                "Media not found yet. photoId={}, mediaId={}",
                latestPhoto.id,
                latestPhoto.mediaId,
            )
            return GetFavoriteSummaryResult(latestImageUrl = null, totalCount = totalCount)
        }

        val latestImageUrl = "${appProperties.server.url}$IMAGE_URL_PATH${media.storageKey}"

        return GetFavoriteSummaryResult(latestImageUrl = latestImageUrl, totalCount = totalCount)
    }

    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }
}
