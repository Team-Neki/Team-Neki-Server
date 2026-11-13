package com.neki.api.pose.api.dto

import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.models.PoseSourceKey
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.CodePointLength

/**
 * fileName       : PoseRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose 관련 요청 DTO
 */
object PoseRequest {
    @Schema(name = "UploadPoseRequest")
    data class UploadPose(@field:Valid val uploads: List<Item>) {
        @Schema(name = "UploadPoseItem")
        data class Item(
            @field:NotNull(message = "mediaId는 필수 입력값입니다.")
            val mediaId: Long?,

            @field:Schema(description = "인원 수", example = "ONE")
            val headCount: HeadCount,

            @field:CodePointLength(max = 255, message = "메모는 최대 255자까지 입력할 수 있습니다.")
            val memo: String?,

            @field:Schema(
                description = "2인 포즈 관계 (FRIEND 친구, COUPLE 커플, SSUM 썸). 선택값이며 headCount 가 TWO 일 때만 보낼 수 있다",
                example = "FRIEND",
                nullable = true,
            )
            val relation: PoseRelation? = null,

            @field:Schema(
                description = "원본 키(PM media_key). 선택. 영문·숫자·._- 1~100자, 대소문자 구분. 이미 등록된 키면 409 D-18",
                example = "friend_001_F1-4_F2-4",
                nullable = true,
            )
            @field:Pattern(regexp = PoseSourceKey.REGEX, message = "sourceKey 형식이 올바르지 않습니다.")
            val sourceKey: String? = null,
        )
    }

    @Schema(name = "UpdatePoseScarpRequest")
    data class UpdatePoseScarp(
        @field:Schema(description = "변경하고자 하는 스크랩 상태", example = "true")
        @field:NotNull(message = "scrap은 필수값입니다.")
        val scrap: Boolean?,
    )
}
