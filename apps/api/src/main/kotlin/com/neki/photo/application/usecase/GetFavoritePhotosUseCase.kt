package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.photo.application.dto.PhotoImageQuery
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.application.port.dto.MediaContract
import com.neki.photo.entity.PhotoImage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetFavoritePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 13. 오후 10:30
 * description    :
 */
@UseCase
class GetFavoritePhotosUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(query: PhotoImageQuery.GetFavoritePhotos): PhotoImageResult.GetPhotos {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = query.size + 1

        val photos: List<PhotoImage> = transactionRunner.readOnly {
            photoImageRepository.listOwnedFavoritePhotos(
                userId = query.userId,
                offset = query.page * query.size,
                limit = fetchSize,
                sortOrder = query.sortOrder,
            )
        }

        val totalCount: Long = photoImageRepository.countOwnedFavoritePhotos(query.userId)

        if (photos.isEmpty()) {
            return PhotoImageResult.GetPhotos(emptyList(), hasNext = false, totalCount = totalCount)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = photos.size > query.size

        // 실제 반환할 사진 목록 (size개만)
        val photosToReturn: List<PhotoImage> = if (hasNext) photos.dropLast(1) else photos

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos: List<MediaContract.StorageInfo> = mediaClient.getMediaStorageInfos(
            query.userId,
            photosToReturn.map { it.mediaId },
        )

        val mediaByFileId: Map<Long, MediaContract.StorageInfo> = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result: List<PhotoImageResult.GetPhotos.Item> = photosToReturn.mapNotNull {
            val media = mediaByFileId[it.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}, userId={}",
                        it.id,
                        it.mediaId,
                        query.userId,
                    )
                    return@mapNotNull null
                }

            PhotoImageResult.GetPhotos.Item(
                photoId = it.id!!,
                storageKey = media.storageKey,
                favorite = true,
                contentType = media.contentType,
                uploadMethod = it.uploadMethod,
                width = media.width,
                height = media.height,
                createdAt = it.createdAt!!,
                capturedAt = it.capturedAt,
            )
        }.toList()

        return PhotoImageResult.GetPhotos(result, hasNext, totalCount)
    }
}
