package com.neki.media.api.dto

import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaCommand
import org.springframework.stereotype.Component

/**
 * fileName       : MediaConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Media api layer converter
 */
object MediaConverter {
    @Component
    class RequestConverter {
        fun toGenerateUploadTicketCommand(
            ownerId: Long,
            request: MediaRequest.UploadTicket,
        ): MediaCommand.GenerateUploadTicket = MediaCommand.GenerateUploadTicket(
            ownerId = ownerId,
            items = request.items.map { item ->
                MediaCommand.GenerateUploadTicket.Item(
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

    @Component
    class ResponseConverter {
        fun toUploadTicketResponse(result: MediaResult.GenerateUploadTicket): MediaResponse.UploadTicket =
            MediaResponse.UploadTicket(
                method = result.method,
                expiresIn = result.expiresAt,
                items = result.tickets.map { ticket ->
                    MediaResponse.UploadTicket.Item(
                        mediaId = ticket.mediaId,
                        uploadTicket = ticket.uploadUrl,
                        contentType = ticket.contentType,
                    )
                },
            )
    }
}
