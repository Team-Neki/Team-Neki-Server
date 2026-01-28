package com.yapp2app.pose.api.dto

import com.yapp2app.pose.domain.HeadCount
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * fileName       : UploadPoseRequest
 * author         : darren
 * date           : 2026. 1. 27. 17:49
 * description    :
 */
data class UploadPoseRequest(val uploads: List<UploadPoseItem>) {
    data class UploadPoseItem(
        @field:NotNull(message = "mediaId는 필수 입력값입니다.")
        val mediaId: Long?,

        @field:Schema(description = "인원 수", example = "ONE")
        val headCount: HeadCount,

        val memo: String?,
    )
}
