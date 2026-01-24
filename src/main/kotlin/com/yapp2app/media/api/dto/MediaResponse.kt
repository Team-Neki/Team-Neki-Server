package com.yapp2app.media.api.dto

import java.time.Instant

/**
 * fileName       : MediaResponse
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:34
 * description    :
 */
data class GenerateUploadTicketResponse(
    val mediaId: Long,
    val uploadUrl: String,
    val method: String,
    val expiresIn: Instant,
    val contentType: String,
)

data class BulkGenerateUploadTicketResponse(val tickets: List<UploadTicketInfo>) {
    data class UploadTicketInfo(
        val mediaId: Long,
        val uploadTicket: String,
        val method: String,
        val expiresIn: Instant,
        val contentType: String,
    )
}
