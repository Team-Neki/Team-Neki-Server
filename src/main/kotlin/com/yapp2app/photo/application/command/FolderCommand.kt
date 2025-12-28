package com.yapp2app.photo.application.command

/**
 * fileName       : FolderCommand
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:44
 * description    : Folder usecase 관련 command
 */
data class CreateFolderCommand(val userId: Long, val name: String)

data class DeleteFolderCommand(val userId: Long, val folderId: Long)

data class DeleteFoldersCommand(val userId: Long, val folderIds: List<Long>)

data class GetFoldersCommand(val userId: Long)

data class UpdateFolderCommand(val userId: Long, val folderId: Long, val name: String)
