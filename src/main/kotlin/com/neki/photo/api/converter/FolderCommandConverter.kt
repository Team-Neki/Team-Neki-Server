package com.neki.photo.api.converter

import com.neki.photo.api.dto.CopyPhotosToFolderRequest
import com.neki.photo.api.dto.CreateFolderRequest
import com.neki.photo.api.dto.DeleteFoldersRequest
import com.neki.photo.api.dto.MovePhotosToFolderRequest
import com.neki.photo.api.dto.RemovePhotosFromFolderRequest
import com.neki.photo.api.dto.UpdateFolderRequest
import com.neki.photo.application.dto.FolderCommand
import com.neki.photo.application.dto.FolderQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FolderCommandConverter
 * author         : koo
 * date           : 2025. 12. 28. 오후 9:44
 * description    :
 */
@Component
class FolderCommandConverter {

    fun toCreateFolderCommand(request: CreateFolderRequest, userId: Long): FolderCommand.CreateFolder =
        FolderCommand.CreateFolder(userId, request.name!!)

    fun toGetFoldersQuery(userId: Long, limit: Int?): FolderQuery.GetFolders = FolderQuery.GetFolders(userId, limit)

    fun toDeleteFoldersCommand(request: DeleteFoldersRequest, userId: Long, deletePhotos: Boolean) =
        FolderCommand.DeleteFolders(userId, request.folderIds, deletePhotos)

    fun toUpdateFolderCommand(request: UpdateFolderRequest, folderId: Long, userId: Long) =
        FolderCommand.UpdateFolder(userId, folderId, request.name!!)

    fun toRemovePhotosFromFolderCommand(request: RemovePhotosFromFolderRequest, folderId: Long, userId: Long) =
        FolderCommand.RemovePhotosFromFolder(userId, folderId, request.photoIds)

    fun toMovePhotosToFolderCommand(request: MovePhotosToFolderRequest, userId: Long) =
        FolderCommand.MovePhotosToFolder(userId, request.sourceFolderId!!, request.photoIds, request.targetFolderIds)

    fun toCopyPhotosToFolderCommand(request: CopyPhotosToFolderRequest, userId: Long) =
        FolderCommand.CopyPhotosToFolder(userId, request.photoIds, request.targetFolderIds)
}
