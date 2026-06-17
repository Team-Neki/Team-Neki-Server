package com.neki.pose.api.dto

import com.neki.pose.HeadCount
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * fileName       : PoseRequest
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

data class UpdatePoseScarpRequest(
    @field:Schema(description = "변경하고자 하는 스크랩 상태", example = "true")
    @field:NotNull(message = "scrap은 필수값입니다.")
    val scrap: Boolean?,
)
