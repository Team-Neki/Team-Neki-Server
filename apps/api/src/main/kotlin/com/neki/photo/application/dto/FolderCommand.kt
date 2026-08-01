package com.neki.photo.application.dto

/**
 * fileName       : FolderCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder usecase 관련 command
 */
object FolderCommand {
    data class CreateFolder(val userId: Long, val name: String)

    data class DeleteFolders(val userId: Long, val folderIds: List<Long>, val deletePhotos: Boolean = false)

    data class UpdateFolder(val userId: Long, val folderId: Long, val newName: String)

    data class RemovePhotosFromFolder(val userId: Long, val folderId: Long, val photoIds: List<Long>)

    data class MovePhotosToFolder(
        val userId: Long,
        val sourceFolderId: Long,
        val photoIds: List<Long>,
        val targetFolderIds: List<Long>,
    )

    data class CopyPhotosToFolder(val userId: Long, val photoIds: List<Long>, val targetFolderIds: List<Long>)
}
