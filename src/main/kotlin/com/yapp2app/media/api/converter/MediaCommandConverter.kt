package com.yapp2app.media.api.converter

import com.yapp2app.media.api.dto.UploadTicketRequest
import com.yapp2app.media.application.command.GenerateUploadTicketCommand
import org.springframework.stereotype.Component

/**
 * fileName       : MediaCommandConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:48
 * description    : Media application layer command 변경을 위한 converter
 */
@Component
class MediaCommandConverter {

    fun toGenerateUploadTicketCommand(ownerId: Long, request: UploadTicketRequest): GenerateUploadTicketCommand =
        GenerateUploadTicketCommand(
            ownerId = ownerId,
            items = request.items.map { item ->
                GenerateUploadTicketCommand.UploadTicketItem(
                    filename = item.filename,
                    contentType = item.contentType,
                    mediaType = item.mediaType!!,
                )
            },
        )
}
