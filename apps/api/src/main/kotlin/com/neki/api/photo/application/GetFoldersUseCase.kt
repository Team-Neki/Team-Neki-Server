package com.neki.api.photo.application

import com.neki.api.photo.application.dto.FolderAssembler
import com.neki.api.photo.application.dto.FolderResult
import com.neki.core.annotation.UseCase
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.dto.FolderQuery
import com.neki.domain.photo.models.FolderStats
import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.service.FolderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFoldersUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:04
 * description    : 폴더 목록 조회 usecase (TODO : 필요에 따라 Paging 추가)
 */
@UseCase
class GetFoldersUseCase(private val folderService: FolderService, private val mediaClient: MediaClient) {

    @Transactional(readOnly = true)
    fun execute(query: FolderQuery.GetFolders): FolderResult.GetFolders {
        val foldersWithStats: List<FolderStats> = folderService.listFoldersWithStats(query)

        val coverMediaIds: List<Long> = foldersWithStats.mapNotNull { it.coverMediaId }
        val medias: List<MediaMetadata> = if (coverMediaIds.isEmpty()) {
            emptyList()
        } else {
            mediaClient.getMediaMetadata(query.userId, coverMediaIds)
        }

        return FolderResult.GetFolders(items = FolderAssembler.toItems(foldersWithStats, medias))
    }
}
