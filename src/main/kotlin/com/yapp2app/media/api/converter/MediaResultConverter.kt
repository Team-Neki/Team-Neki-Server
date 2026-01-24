package com.yapp2app.media.api.converter

import com.yapp2app.media.api.dto.UploadTicketResponse
import com.yapp2app.media.application.result.GenerateUploadTicketResult
import org.springframework.stereotype.Component

/**
 * fileName       : MediaResultConverter
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:48
 * description    : Media api layer response 변경을 위한 converter
 */
@Component
class MediaResultConverter {

    fun toUploadTicketResponse(result: GenerateUploadTicketResult): UploadTicketResponse = UploadTicketResponse(
        items = result.tickets.map { ticket ->
            UploadTicketResponse.UploadTicketInfo(
                mediaId = ticket.mediaId,
                uploadTicket = ticket.uploadUrl,
                method = ticket.method,
                expiresIn = ticket.expiresAt,
                contentType = ticket.contentType,
            )
        },
    )
}
