package com.yapp2app.media.api.dto

import com.yapp2app.media.domain.MediaType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * fileName       : MediaRequest
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:33
 * description    :
 */
data class GenerateUploadTicketRequest(val filename: String, val contentType: String, val mediaType: MediaType)

data class BulkGenerateUploadTicketRequest(
    @field:Valid
    @field:NotEmpty(message = "업로드 항목이 비어있습니다.")
    @field:Size(min = 1, max = 10, message = "한 번에 1개에서 10개까지 업로드할 수 있습니다.")
    val items: List<UploadTicketItem>,
) {
    data class UploadTicketItem(
        @field:NotBlank(message = "파일명은 필수 입력값입니다.")
        val filename: String,

        @field:NotBlank(message = "Content type은 필수 입력값입니다.")
        val contentType: String,

        @field:NotNull(message = "미디어 타입은 필수 입력값입니다.")
        val mediaType: MediaType?,
    )
}
