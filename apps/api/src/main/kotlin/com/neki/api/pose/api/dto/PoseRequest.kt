package com.neki.api.pose.api.dto

import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.models.PoseSourceKey
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
    @Schema(
        name = "GetPosesRequest",
        description = "포즈 목록 요청. 필터는 filterGroup 으로 보냅니다.",
        example = """
            {
                "page": 0,
                "size": 20,
                "filterGroup": {
                    "headCountFilter": { "headCount": "TWO" },
                    "relationFilter": { "relation": "FRIEND" }
                }
            }
        """,
    )
    data class GetPoses(
        @field:Schema(description = "페이지 번호 (0부터)", example = "0")
        @field:Min(value = 0, message = "page는 0 이상이어야 합니다.")
        val page: Int = 0,

        @field:Schema(description = "페이지 크기 (1~100)", example = "20")
        @field:Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @field:Max(value = 100, message = "size는 100 이하여야 합니다.")
        val size: Int = 20,

        @field:Schema(description = "필터 그룹. 필수이며 필터를 안 걸려면 {} 를 보냅니다")
        @field:Valid
        val filterGroup: FilterGroup,
    )

    @Schema(
        name = "PoseFilterGroupRequest",
        description = "포즈 목록 필터 그룹. 필터가 늘면 여기에 그룹이 추가됩니다.",
    )
    data class FilterGroup(
        @field:Schema(description = "인원 수 필터. 없으면 모든 인원 수")
        @field:Valid
        val headCountFilter: HeadCountFilter? = null,

        @field:Schema(description = "관계 필터 (2인 탭). 없으면 관계 없는 포즈를 포함해 전체")
        @field:Valid
        val relationFilter: RelationFilter? = null,
    ) {
        @Schema(name = "PoseHeadCountFilterRequest")
        data class HeadCountFilter(
            @field:Schema(
                description = "인원 수 (ONE, TWO, THREE, FOUR, FIVE_OR_MORE). null 이면 전체",
                example = "TWO",
                nullable = true,
            )
            val headCount: HeadCount? = null,
        )

        @Schema(name = "PoseRelationFilterRequest")
        data class RelationFilter(
            @field:Schema(
                description = "관계 (FRIEND 친구, COUPLE 커플, SSUM 썸). null 이면 전체. " +
                    "관계는 2인 포즈에만 있으므로 TWO 가 아닌 headCount 와 함께 보내면 빈 목록",
                example = "FRIEND",
                nullable = true,
            )
            val relation: PoseRelation? = null,
        )
    }

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
