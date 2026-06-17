package com.neki.media.api.converter

import com.neki.media.api.dto.UploadTicketResponse
import com.neki.media.application.result.GenerateUploadTicketResult
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
