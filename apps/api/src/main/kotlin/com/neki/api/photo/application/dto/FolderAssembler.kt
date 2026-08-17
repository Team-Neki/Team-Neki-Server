package com.neki.api.photo.application.dto

import com.neki.domain.photo.models.FolderStats
import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.models.MediaMetadatas

/**
 * fileName       : FolderAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 폴더 조회 결과에 media 메타데이터를 붙여 응답 항목으로 조립한다.
 */
object FolderAssembler {

    fun toItems(foldersWithStats: List<FolderStats>, medias: List<MediaMetadata>): List<FolderResult.GetFolders.Item> {
        val metadatas = MediaMetadatas(medias)

        return foldersWithStats.map { folder ->
            FolderResult.GetFolders.Item(
                folderId = folder.folderId,
                name = folder.name,
                storageKey = folder.coverMediaId?.let { metadatas[it]?.storageKey },
                count = folder.photoCount,
            )
        }
    }
}
