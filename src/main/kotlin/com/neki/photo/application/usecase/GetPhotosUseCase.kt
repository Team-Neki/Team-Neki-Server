package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.photo.application.command.GetPhotosCommand
import com.neki.photo.application.contract.MediaStorageInfo
import com.neki.photo.application.contract.PhotoWithFavorite
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.application.result.GetPhotosResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.dropLast
import kotlin.collections.map
import kotlin.collections.mapNotNull

/**
 * fileName       : GetPhotosUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 3:29
 * description    : photoImage 목록 조회
 */
@UseCase
class GetPhotosUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetPhotosCommand): GetPhotosResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val photosWithFavorite: List<PhotoWithFavorite> = photoImageRepository.listOwnedPhotosWithFavorite(
            userId = command.userId,
            folderId = command.folderId,
            offset = command.page * command.size,
            limit = fetchSize,
            sortOrder = command.sortOrder,
        )

        if (photosWithFavorite.isEmpty()) {
            return GetPhotosResult(emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = photosWithFavorite.size > command.size

        // 실제 반환할 사진 목록 (size개만)
        val photosToReturn: List<PhotoWithFavorite> = if (hasNext) {
            photosWithFavorite.dropLast(
                1,
            )
        } else {
            photosWithFavorite
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            command.userId,
            photosToReturn.map { it.photo.mediaId },
        )

        val mediaByFileId: Map<Long, MediaStorageInfo> = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result: List<GetPhotosResult.PhotoInfo> = photosToReturn.mapNotNull { (photo, isFavorite) ->
            val media = mediaByFileId[photo.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}, userId={}",
                        photo.id,
                        photo.mediaId,
                        command.userId,
                    )
                    return@mapNotNull null
                }

            GetPhotosResult.PhotoInfo(
                photoId = photo.id!!,
                storageKey = media.storageKey,
                favorite = isFavorite,
                contentType = media.contentType,
                uploadMethod = photo.uploadMethod,
                width = media.width,
                height = media.height,
                createdAt = photo.createdAt!!,
                capturedAt = photo.capturedAt,
            )
        }

        return GetPhotosResult(result, hasNext)
    }
}
