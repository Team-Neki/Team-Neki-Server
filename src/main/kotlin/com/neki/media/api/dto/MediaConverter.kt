package com.neki.media.api.dto

import com.neki.media.application.dto.MediaCommand
import com.neki.media.application.dto.MediaResult
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
            request: UploadTicketRequest,
        ): MediaCommand.GenerateUploadTicket = MediaCommand.GenerateUploadTicket(
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

    @Component
    class ResponseConverter {
        fun toUploadTicketResponse(result: MediaResult.GenerateUploadTicket): UploadTicketResponse =
            UploadTicketResponse(
                method = result.method,
                expiresIn = result.expiresAt,
                items = result.tickets.map { ticket ->
                    UploadTicketResponse.UploadTicketInfo(
                        mediaId = ticket.mediaId,
                        uploadTicket = ticket.uploadUrl,
                        contentType = ticket.contentType,
                    )
                },
            )
    }
}
