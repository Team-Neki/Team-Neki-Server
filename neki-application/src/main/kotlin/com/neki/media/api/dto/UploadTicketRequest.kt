package com.neki.media.api.dto

import com.neki.media.MediaType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * fileName       : UploadTicketRequest
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:33
 * description    :
 */
data class UploadTicketRequest(
    @field:Valid
    @field:NotEmpty(message = "업로드 항목이 비어있습니다.")
    @field:Size(min = 1, max = 10, message = "한 번에 1개에서 10개까지 업로드할 수 있습니다.")
    val items: List<UploadTicketItem>,
) {
    data class UploadTicketItem(
        @field:Schema(description = "파일명", example = "abc.png")
        @field:NotBlank(message = "파일명은 필수 입력값입니다.")
        val filename: String?,

        @field:Schema(description = "ContentType", example = "image/png")
        @field:NotBlank(message = "Content type은 필수 입력값입니다.")
        val contentType: String?,

        @field:Schema(description = "미디어타입", example = "PHOTO_BOOTH")
        @field:NotNull(message = "미디어 타입은 필수 입력값입니다.")
        val mediaType: MediaType?,

        @field:Schema(description = "너비", example = "1920")
        val width: Int? = null,

        @field:Schema(description = "높이", example = "1080")
        val height: Int? = null,

        @field:Schema(description = "byte 단위", example = "2048576")
        val size: Long? = null,
    )
}
