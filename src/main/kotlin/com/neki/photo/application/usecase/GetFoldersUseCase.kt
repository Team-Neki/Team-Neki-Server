package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.photo.application.command.GetFoldersCommand
import com.neki.photo.application.contract.FolderWithStats
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.result.GetFoldersResult
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
    fun execute(command: GetFoldersCommand): GetFoldersResult {
        val foldersWithStats: List<FolderWithStats> = folderRepository.listOwnedFoldersWithStats(
            command.userId,
            command.limit,
        )

        val items: List<GetFoldersResult.FolderInfo> = foldersWithStats.map { folder ->
            GetFoldersResult.FolderInfo(
                folderId = folder.folderId,
                name = folder.name,
                storageKey = folder.coverImageStorageKey,
                count = folder.photoCount,
            )
        }

        return GetFoldersResult(items = items)
    }
}
