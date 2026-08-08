package com.neki.api.photo.application.dto

import com.neki.domain.photo.models.FolderStats

/**
 * fileName       : FolderAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 폴더 조회 결과를 응답 항목으로 조립한다.
 */
object FolderAssembler {

    fun toItems(foldersWithStats: List<FolderStats>): List<FolderResult.GetFolders.Item> =
        foldersWithStats.map { folder ->
            FolderResult.GetFolders.Item(
                folderId = folder.folderId,
                name = folder.name,
                storageKey = folder.coverImageStorageKey,
                count = folder.photoCount,
            )
        }
}
