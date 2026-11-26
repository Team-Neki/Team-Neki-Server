package com.neki.api.share.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * fileName       : ShareLinkRequest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크 관련 요청 DTO
 */
object ShareLinkRequest {
    @Schema(name = "IssueShareLinkRequest")
    data class IssueShareLink(
        @field:Schema(description = "공유할 사진 ID", example = "1")
        @field:NotNull(message = "photoId는 필수 입력값입니다.")
        @field:Positive(message = "photoId는 양수여야 합니다.")
        val photoId: Long?,
    )
}
