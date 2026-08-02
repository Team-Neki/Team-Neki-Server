package com.neki.photo.application.dto

/**
 * fileName       : FolderQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder usecase 관련 query
 */
object FolderQuery {
    data class GetFolders(val userId: Long, val limit: Int?)
}
