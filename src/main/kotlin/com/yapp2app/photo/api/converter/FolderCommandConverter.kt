package com.yapp2app.photo.api.converter

import com.yapp2app.photo.api.dto.CreateFolderRequest
import com.yapp2app.photo.api.dto.DeleteFoldersRequest
import com.yapp2app.photo.api.dto.UpdateFolderRequest
import com.yapp2app.photo.application.command.CreateFolderCommand
import com.yapp2app.photo.application.command.DeleteFoldersCommand
import com.yapp2app.photo.application.command.GetFoldersCommand
import com.yapp2app.photo.application.command.UpdateFolderCommand
import org.springframework.stereotype.Component

/**
 * fileName       : FolderCommandConverter
 * author         : koo
 * date           : 2025. 12. 28. 오후 9:44
 * description    :
 */
@Component
class FolderCommandConverter {

    fun toCreateFolderCommand(request: CreateFolderRequest, userId: Long): CreateFolderCommand =
        CreateFolderCommand(userId, request.name)

    fun toGetFoldersCommand(userId: Long): GetFoldersCommand = GetFoldersCommand(userId)

    fun toDeleteFoldersCommand(request: DeleteFoldersRequest, userId: Long) =
        DeleteFoldersCommand(userId, request.folderIds)

    fun toUpdateFolderCommand(request: UpdateFolderRequest, folderId: Long, userId: Long) =
        UpdateFolderCommand(userId, folderId, request.name)
}
