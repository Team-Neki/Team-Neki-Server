package com.yapp2app.media.api.dto

import com.yapp2app.media.domain.MediaType

/**
 * fileName       : GenerateUploadTicketRequest
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:47
 * description    : object storage 저장 요청
 */
data class GenerateUploadTicketRequest(val filename: String, val contentType: String, val mediaType: MediaType)
