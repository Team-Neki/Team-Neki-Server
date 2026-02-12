package com.yapp2app.media.application.command

import com.yapp2app.media.domain.MediaType

/**
 * fileName       : MediaCommand
 * author         : koo
 * date           : 2026. 1. 3. 오전 12:04
 * description    : Media domain command
 */
data class ConfirmMediasUploadedCommand(val ownerId: Long, val mediaIds: List<Long>)

data class GenerateUploadTicketCommand(val ownerId: Long, val items: List<UploadTicketItem>) {
    data class UploadTicketItem(
        val filename: String,
        val contentType: String,
        val mediaType: MediaType,
        val width: Int? = null,
        val height: Int? = null,
        val size: Long? = null,
    )
}

data class DeleteMediaCommand(val ownerId: Long, val mediaId: Long)

data class DeleteMediasCommand(val ownerId: Long, val mediaIds: List<Long>)

data class GetMediasCommand(val ownerId: Long, val mediaIds: List<Long>)

data class GetImageByKeyCommand(val objectKey: String)

data class GetMediaStorageInfoCommand(val ownerId: Long?, val mediaId: Long)

data class GetMediaStorageInfosCommand(val ownerId: Long?, val mediaIds: List<Long>)
