package com.neki.photo.application

import com.neki.common.annotation.UseCase
import com.neki.photo.application.dto.FolderAssembler
import com.neki.photo.application.dto.FolderResult
import com.neki.photo.dto.FolderQuery
import com.neki.photo.models.FolderStats
import com.neki.photo.service.FolderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFoldersUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:04
 * description    : 폴더 목록 조회 usecase (TODO : 필요에 따라 Paging 추가)
 */
@UseCase
class GetFoldersUseCase(private val folderService: FolderService) {

    @Transactional(readOnly = true)
    fun execute(query: FolderQuery.GetFolders): FolderResult.GetFolders {
        val foldersWithStats: List<FolderStats> = folderService.listFoldersWithStats(query)

        return FolderResult.GetFolders(items = FolderAssembler.toItems(foldersWithStats))
    }
}
