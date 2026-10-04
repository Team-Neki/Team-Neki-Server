package com.neki.api.media.api.dto

import com.neki.api.media.application.dto.MediaResult
import com.neki.domain.media.dto.MediaCommand
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

        fun toGenerateQrDumpUploadTicketCommand(ownerId: Long): MediaCommand.GenerateQrDumpUploadTicket =
            MediaCommand.GenerateQrDumpUploadTicket(ownerId = ownerId)
    }

    @Component
    class ResponseConverter {
        fun toQrDumpUploadTicketResponse(
            result: MediaResult.GenerateQrDumpUploadTicket,
        ): MediaResponse.QrDumpUploadTicket = MediaResponse.QrDumpUploadTicket(
            method = result.method,
            expiresIn = result.expiresAt,
            uploadTicket = result.uploadUrl,
            contentType = result.contentType,
        )

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
