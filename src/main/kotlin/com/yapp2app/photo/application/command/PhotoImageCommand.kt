package com.yapp2app.photo.application.command

/**
 * fileName       : PhotoImageCommand
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain command
 */
data class UploadPhotoCommand(val userId: Long, val mediaId: Long, val folderId: Long?)

data class GetPhotosCommand(val userId: Long, val folderId: Long?)

data class DeletePhotoCommand(val userId: Long, val photoId: Long)

data class DeletePhotosCommand(val userId: Long, val photoIds: List<Long>)
