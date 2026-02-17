package com.yapp2app.photo.application.command

/**
 * fileName       : FolderCommand
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:44
 * description    : Folder usecase 관련 command
 */
data class CreateFolderCommand(val userId: Long, val name: String)

data class DeleteFoldersCommand(val userId: Long, val folderIds: List<Long>, val deletePhotos: Boolean = false)

data class GetFoldersCommand(val userId: Long, val limit: Int?)

data class UpdateFolderCommand(val userId: Long, val folderId: Long, val newName: String)

data class RemovePhotosFromFolderCommand(val userId: Long, val folderId: Long, val photoIds: List<Long>)
