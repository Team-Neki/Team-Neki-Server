package com.neki.api.pose.api.dto

import com.neki.domain.pose.models.HeadCount
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * fileName       : PoseRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose 관련 요청 DTO
 */
object PoseRequest {
    @Schema(name = "UploadPoseRequest")
    data class UploadPose(val uploads: List<Item>) {
        @Schema(name = "UploadPoseItem")
        data class Item(
            @field:NotNull(message = "mediaId는 필수 입력값입니다.")
            val mediaId: Long?,

            @field:Schema(description = "인원 수", example = "ONE")
            val headCount: HeadCount,

            val memo: String?,
        )
    }

    @Schema(name = "UpdatePoseScarpRequest")
    data class UpdatePoseScarp(
        @field:Schema(description = "변경하고자 하는 스크랩 상태", example = "true")
        @field:NotNull(message = "scrap은 필수값입니다.")
        val scrap: Boolean?,
    )
}
