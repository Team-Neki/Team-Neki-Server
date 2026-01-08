package com.yapp2app.media.api.dto

/**
 * fileName       : GenerateUploadTicketResponse
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:47
 * description    : object storage 저장 응답
 */
data class GenerateUploadTicketResponse(val mediaId: Long, val presignedUrl: String)
