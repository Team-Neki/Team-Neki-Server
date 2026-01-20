package com.yapp2app.media.api.converter

import com.yapp2app.media.api.dto.GenerateUploadTicketRequest
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

    fun toGenerateUploadTicketCommand(
        ownerId: Long,
        request: GenerateUploadTicketRequest,
    ): GenerateUploadTicketCommand = GenerateUploadTicketCommand(
        ownerId = ownerId,
        filename = request.filename,
        contentType = request.contentType,
        mediaType = request.mediaType,
    )
}
