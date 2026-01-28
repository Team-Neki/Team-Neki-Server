package com.yapp2app.photo.application.contract

/**
 * fileName       : FolderWithStats
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : Folder with aggregated statistics (photo count and cover image info)
 */
data class FolderWithStats(
    val folderId: Long,
    val name: String,
    val coverPhotoMediaId: Long?,
    val latestPhotoMediaId: Long?,
    val photoCount: Long,
)
