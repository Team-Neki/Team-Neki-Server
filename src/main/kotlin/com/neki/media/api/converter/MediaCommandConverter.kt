package com.neki.media.api.converter

import com.neki.media.api.dto.UploadTicketRequest
import com.neki.media.application.dto.MediaCommand
import org.springframework.stereotype.Component

/**
 * fileName       : MediaCommandConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:48
 * description    : Media application layer command 변경을 위한 converter
 */
@Component
class MediaCommandConverter {

    fun toGenerateUploadTicketCommand(ownerId: Long, request: UploadTicketRequest): MediaCommand.GenerateUploadTicket =
        MediaCommand.GenerateUploadTicket(
            ownerId = ownerId,
            items = request.items.map { item ->
                MediaCommand.GenerateUploadTicket.UploadTicketItem(
                    filename = item.filename!!,
                    contentType = item.contentType!!,
                    mediaType = item.mediaType!!,
                    width = item.width,
                    height = item.height,
                    size = item.size,
                )
            },
        )
}
