package com.neki.photo.api.converter

import com.neki.photo.api.dto.CopyPhotosToFolderRequest
import com.neki.photo.api.dto.CreateFolderRequest
import com.neki.photo.api.dto.DeleteFoldersRequest
import com.neki.photo.api.dto.MovePhotosToFolderRequest
import com.neki.photo.api.dto.RemovePhotosFromFolderRequest
import com.neki.photo.api.dto.UpdateFolderRequest
import com.neki.photo.application.command.CopyPhotosToFolderCommand
import com.neki.photo.application.command.CreateFolderCommand
import com.neki.photo.application.command.DeleteFoldersCommand
import com.neki.photo.application.command.GetFoldersCommand
import com.neki.photo.application.command.MovePhotosToFolderCommand
import com.neki.photo.application.command.RemovePhotosFromFolderCommand
import com.neki.photo.application.command.UpdateFolderCommand
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
        CreateFolderCommand(userId, request.name!!)

    fun toGetFoldersCommand(userId: Long, limit: Int?): GetFoldersCommand = GetFoldersCommand(userId, limit)

    fun toDeleteFoldersCommand(request: DeleteFoldersRequest, userId: Long, deletePhotos: Boolean) =
        DeleteFoldersCommand(userId, request.folderIds, deletePhotos)

    fun toUpdateFolderCommand(request: UpdateFolderRequest, folderId: Long, userId: Long) =
        UpdateFolderCommand(userId, folderId, request.name!!)

    fun toRemovePhotosFromFolderCommand(request: RemovePhotosFromFolderRequest, folderId: Long, userId: Long) =
        RemovePhotosFromFolderCommand(userId, folderId, request.photoIds)

    fun toMovePhotosToFolderCommand(request: MovePhotosToFolderRequest, sourceFolderId: Long, userId: Long) =
        MovePhotosToFolderCommand(userId, sourceFolderId, request.photoIds, request.targetFolderId)

    fun toCopyPhotosToFolderCommand(request: CopyPhotosToFolderRequest, sourceFolderId: Long, userId: Long) =
        CopyPhotosToFolderCommand(userId, sourceFolderId, request.photoIds, request.targetFolderId)
}
