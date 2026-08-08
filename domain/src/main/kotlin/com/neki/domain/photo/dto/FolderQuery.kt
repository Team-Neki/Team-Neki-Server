package com.neki.domain.photo.dto

/**
 * fileName       : FolderQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder usecase 관련 query
 */
object FolderQuery {
    data class GetFolders(override val userId: Long, val limit: Int?) : UserScoped
}
