package com.neki.photo.api.dto

import com.neki.common.properties.AppProperties
import com.neki.photo.application.dto.FolderResult
import com.neki.photo.dto.FolderCommand
import com.neki.photo.dto.FolderQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FolderConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder api layer converter
 */
object FolderConverter {
    @Component
    class RequestConverter {
        fun toCreateFolderCommand(request: FolderRequest.CreateFolder, userId: Long): FolderCommand.CreateFolder =
            FolderCommand.CreateFolder(userId, request.name!!)

        fun toGetFoldersQuery(userId: Long, limit: Int?): FolderQuery.GetFolders = FolderQuery.GetFolders(userId, limit)

        fun toDeleteFoldersCommand(request: FolderRequest.DeleteFolders, userId: Long, deletePhotos: Boolean) =
            FolderCommand.DeleteFolders(userId, request.folderIds, deletePhotos)

        fun toUpdateFolderCommand(request: FolderRequest.UpdateFolder, folderId: Long, userId: Long) =
            FolderCommand.UpdateFolder(userId, folderId, request.name!!)

        fun toRemovePhotosFromFolderCommand(
            request: FolderRequest.RemovePhotosFromFolder,
            folderId: Long,
            userId: Long,
        ) = FolderCommand.RemovePhotosFromFolder(userId, folderId, request.photoIds)

        fun toMovePhotosToFolderCommand(request: FolderRequest.MovePhotosToFolder, userId: Long) =
            FolderCommand.MovePhotosToFolder(
                userId,
                request.sourceFolderId!!,
                request.photoIds,
                request.targetFolderIds,
            )

        fun toCopyPhotosToFolderCommand(request: FolderRequest.CopyPhotosToFolder, userId: Long) =
            FolderCommand.CopyPhotosToFolder(userId, request.photoIds, request.targetFolderIds)
    }

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
        }

        fun toGetAllFoldersResponse(result: FolderResult.GetFolders): FolderResponse.GetAllFolder =
            FolderResponse.GetAllFolder(
                items = result.items.map {
                    FolderResponse.GetAllFolder.Item(
                        it.folderId,
                        it.name,
                        latestImageUrl = it.storageKey?.let { key -> toImageUrl(key) },
                        totalCount = it.count,
                    )
                },

            )

        fun toCreateFolderResponse(result: FolderResult.CreateFolder): FolderResponse.CreateFolder =
            FolderResponse.CreateFolder(result.folderId)

        private fun toImageUrl(objectKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$objectKey"
    }
}
