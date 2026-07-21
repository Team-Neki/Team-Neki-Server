package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.photo.application.dto.PhotoImageQuery
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.application.port.dto.MediaContract
import com.neki.photo.application.port.dto.PhotoContract
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

    fun execute(query: PhotoImageQuery.GetPhotos): PhotoImageResult.GetPhotos {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = query.size + 1

        val photosWithFavorite: List<PhotoContract.PhotoWithFavorite> =
            photoImageRepository.listOwnedPhotosWithFavorite(
                userId = query.userId,
                folderId = query.folderId,
                offset = query.page * query.size,
                limit = fetchSize,
                sortOrder = query.sortOrder,
            )

        val totalCount: Long = photoImageRepository.countOwnedPhotos(
            userId = query.userId,
            folderId = query.folderId,
        )

        if (photosWithFavorite.isEmpty()) {
            return PhotoImageResult.GetPhotos(emptyList(), hasNext = false, totalCount = totalCount)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = photosWithFavorite.size > query.size

        // 실제 반환할 사진 목록 (size개만)
        val photosToReturn: List<PhotoContract.PhotoWithFavorite> = if (hasNext) {
            photosWithFavorite.dropLast(
                1,
            )
        } else {
            photosWithFavorite
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos: List<MediaContract.StorageInfo> = mediaClient.getMediaStorageInfos(
            query.userId,
            photosToReturn.map { it.photo.mediaId },
        )

        val mediaByFileId: Map<Long, MediaContract.StorageInfo> = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result: List<PhotoImageResult.GetPhotos.PhotoInfo> = photosToReturn.mapNotNull { (photo, isFavorite) ->
            val media = mediaByFileId[photo.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}, userId={}",
                        photo.id,
                        photo.mediaId,
                        query.userId,
                    )
                    return@mapNotNull null
                }

            PhotoImageResult.GetPhotos.PhotoInfo(
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

        return PhotoImageResult.GetPhotos(result, hasNext, totalCount)
    }
}
