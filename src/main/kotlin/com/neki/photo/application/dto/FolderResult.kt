package com.neki.photo.application.dto

/**
 * fileName       : FolderResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder usecase 관련 result
 */
object FolderResult {
    data class CreateFolder(val folderId: Long)

    data class GetFolders(val items: List<FolderInfo>) {
        data class FolderInfo(val folderId: Long, val name: String, val storageKey: String?, val count: Long)
    }
}
