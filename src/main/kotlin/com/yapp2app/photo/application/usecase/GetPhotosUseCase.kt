package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.photo.application.command.GetPhotosCommand
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.application.result.GetPhotosResult
import org.slf4j.LoggerFactory

/**
 * fileName       : GetPhotosUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 3:29
 * description    : photoImage 목록 조회
 * TODO : 요구사항 변경에 따라 paging 추가 가능성 있음
 */
@UseCase
class GetPhotosUseCase(
    private val photoImageRepository: PhotoImageRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetPhotosCommand): GetPhotosResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val photos = transactionRunner.readOnly {
            photoImageRepository.listOwnedPhotos(
                userId = command.userId,
                folderId = command.folderId,
                offset = command.page * command.size,
                limit = fetchSize,
            )
        }

        if (photos.isEmpty()) {
            return GetPhotosResult(emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = photos.size > command.size

        // 실제 반환할 사진 목록 (size개만)
        val photosToReturn = if (hasNext) photos.dropLast(1) else photos

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos = mediaClient.getMediaStorageInfos(
            command.userId,
            photosToReturn.map { it.mediaId },
        )

        val mediaByFileId = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result = photosToReturn.mapNotNull {
            val media = mediaByFileId[it.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}, userId={}",
                        it.id,
                        it.mediaId,
                        command.userId,
                    )
                    return@mapNotNull null
                }

            GetPhotosResult.PhotoInfo(
                photoId = it.id!!,
                storageKey = media.storageKey,
                folderId = it.folderId,
                contentType = media.contentType,
                createdAt = it.createdAt.toString(),
            )
        }.toList()

        return GetPhotosResult(result, hasNext)
    }
}
