package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.photo.application.dto.FolderQuery
import com.neki.photo.application.dto.FolderResult
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.dto.PhotoContract
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFoldersUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:04
 * description    : 폴더 목록 조회 usecase (TODO : 필요에 따라 Paging 추가)
 */
@UseCase
class GetFoldersUseCase(private val folderRepository: FolderRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(query: FolderQuery.GetFolders): FolderResult.GetFolders {
        val foldersWithStats: List<PhotoContract.FolderWithStats> = folderRepository.listOwnedFoldersWithStats(
            query.userId,
            query.limit,
        )

        val items: List<FolderResult.GetFolders.FolderInfo> = foldersWithStats.map { folder ->
            FolderResult.GetFolders.FolderInfo(
                folderId = folder.folderId,
                name = folder.name,
                storageKey = folder.coverImageStorageKey,
                count = folder.photoCount,
            )
        }

        return FolderResult.GetFolders(items = items)
    }
}
