package com.yapp2app.media.api.dto

import java.time.Instant

/**
 * fileName       : UploadTicketResponse
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:34
 * description    :
 */
data class UploadTicketResponse(val method: String, val expiresIn: Instant, val items: List<UploadTicketInfo>) {
    data class UploadTicketInfo(val mediaId: Long, val uploadTicket: String, val contentType: String)
}
