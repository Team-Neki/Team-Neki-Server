package com.yapp2app.media.api.converter

import com.yapp2app.media.api.dto.GenerateUploadTicketResponse
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

    fun toGenerateUploadTicketResponse(result: GenerateUploadTicketResult): GenerateUploadTicketResponse =
        GenerateUploadTicketResponse(
            mediaId = result.mediaId,
            presignedUrl = result.presignedUrl,
        )
}
