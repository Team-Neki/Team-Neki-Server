package com.neki.media.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * fileName       : UploadTicketResponse
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:34
 * description    :
 */
data class UploadTicketResponse(
    @field:Schema(description = "요청 Method", example = "PUT")
    val method: String,
    @field:Schema(description = "만료일자", example = "2025-12-23T07:09:00")
    val expiresIn: Instant,
    @field:Schema(description = "목록")
    val items: List<UploadTicketInfo>,
) {
    data class UploadTicketInfo(
        @field:Schema(description = "Media ID", example = "1")
        val mediaId: Long,
        @field:Schema(
            description = "Presigned URL",
            example = "https://https://yapp-neki-staging-ap-northeast-2.s3.ap-northeas...",
        )
        val uploadTicket: String,
        @field:Schema(description = "파일 형식", example = "image/jpeg")
        val contentType: String,
    )
}
