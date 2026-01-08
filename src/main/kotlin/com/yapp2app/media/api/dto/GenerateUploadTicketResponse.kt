package com.yapp2app.media.api.dto

import java.time.Instant

/**
 * fileName       : GenerateUploadTicketResponse
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:47
 * description    : object storage 저장 응답
 */
data class GenerateUploadTicketResponse(
    val mediaId: Long,
    val uploadUrl: String,
    val method: String,
    val expiresIn: Instant,
    val contentType: String,
)
