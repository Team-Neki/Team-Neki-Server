package com.neki.photo.application.port.dto

import com.neki.photo.entity.PhotoImage

/**
 * fileName       : PhotoContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : Photo repository port 계약 타입 (조회 프로젝션)
 */
object PhotoContract {
    data class PhotoWithFavorite(val photo: PhotoImage, val isFavorite: Boolean)

    data class FolderWithStats(
        val folderId: Long,
        val name: String,
        val coverImageStorageKey: String?,
        val photoCount: Long,
    )
}
