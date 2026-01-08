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
        val photos = transactionRunner.readOnly {
            photoImageRepository.listOwnedPhotos(
                userId = command.userId,
                folderId = command.folderId,
            )
        }

        if (photos.isEmpty()) {
            return GetPhotosResult(emptyList())
        }

        // 실제 binary 조회
        val mediaContents = mediaClient.getMediaBinaries(command.userId, photos.map { it.mediaId })

        val mediaByFileId = mediaContents.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result = photos.mapNotNull {
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
                imageBinary = media.binaryData,
                folderId = it.folderId,
                createdAt = it.createdAt.toString(),
            )
        }.toList()

        return GetPhotosResult(result)
    }
}
