package com.neki.domain.media.dto

import com.neki.domain.media.models.MediaType

/**
 * fileName       : MediaCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Media domain command
 */
object MediaCommand {
    data class ConfirmMediasUploaded(val ownerId: Long, val mediaIds: List<Long>)

    data class GenerateUploadTicket(val ownerId: Long, val items: List<Item>) {
        data class Item(
            val filename: String,
            val contentType: String,
            val mediaType: MediaType,
            val width: Int? = null,
            val height: Int? = null,
            val size: Long? = null,
        )
    }

    data class DeleteMedias(val ownerId: Long, val mediaIds: List<Long>)
}
