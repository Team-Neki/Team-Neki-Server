package com.yapp2app.photo.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.photo.application.command.GetFoldersCommand
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.result.GetFoldersResult
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFoldersUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:04
 * description    : 폴더 목록 조회 usecase (TODO : 필요에 따라 Paging 추가)
 */
@UseCase
class GetFoldersUseCase(private val folderRepository: FolderRepositoryPort, private val mediaClient: MediaClientPort) {

    @Transactional(readOnly = true)
    fun execute(command: GetFoldersCommand): GetFoldersResult {
        val foldersWithStats = folderRepository.listOwnedFoldersWithStats(command.userId)

        val mediaIds = foldersWithStats
            .mapNotNull { it.coverPhotoMediaId ?: it.latestPhotoMediaId }
            .distinct()

        val mediaStorageInfoMap = if (mediaIds.isNotEmpty()) {
            mediaClient.getMediaStorageInfos(command.userId, mediaIds)
                .associateBy { it.mediaId }
        } else {
            emptyMap()
        }

        val items = foldersWithStats.map { folder ->
            val mediaId = folder.coverPhotoMediaId ?: folder.latestPhotoMediaId
            val storageKey = mediaId?.let { mediaStorageInfoMap[it]?.storageKey }

            GetFoldersResult.FolderInfo(
                folderId = folder.folderId,
                name = folder.name,
                imageObjectKey = storageKey,
                count = folder.photoCount,
            )
        }

        return GetFoldersResult(items = items)
    }
}
