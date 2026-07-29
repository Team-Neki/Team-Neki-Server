package com.neki.pose.api.dto

import com.neki.pose.domain.HeadCount
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : PoseResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose 관련 응답 DTO
 */
object PoseResponse {
    @Schema(name = "GetPosesResponse")
    data class GetPoses(
        @field:Schema(description = "포즈 목록")
        val items: List<Item>,
        @field:Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNext: Boolean,
    ) {
        @Schema(name = "PoseInfo")
        data class Item(
            @field:Schema(description = "포즈 ID", example = "1")
            val poseId: Long,
            @field:Schema(description = "인원 수", example = "ONE")
            val headCount: HeadCount,
            @field:Schema(description = "사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
            val imageUrl: String,
            @field:Schema(description = "스크랩 여부", example = "true")
            val scrap: Boolean,
            @field:Schema(description = "파일 형식", example = "image/jpeg")
            val contentType: String,
            @field:Schema(description = "이미지 너비", example = "1080", nullable = true)
            val width: Int? = null,
            @field:Schema(description = "이미지 높이", example = "1440", nullable = true)
            val height: Int? = null,
            @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
            val createdAt: LocalDateTime,
        )
    }

    @Schema(name = "GetPoseResponse")
    data class GetPose(
        @field:Schema(description = "포즈 ID", example = "1")
        val poseId: Long,
        @field:Schema(description = "인원 수", example = "ONE")
        val headCount: HeadCount,
        @field:Schema(description = "사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
        val imageUrl: String,
        @field:Schema(description = "스크랩 여부", example = "true")
        val scrap: Boolean,
        @field:Schema(description = "파일 형식", example = "image/jpeg")
        val contentType: String,
        @field:Schema(description = "이미지 너비", example = "1080", nullable = true)
        val width: Int? = null,
        @field:Schema(description = "이미지 높이", example = "1440", nullable = true)
        val height: Int? = null,
        @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
        val createdAt: LocalDateTime,
    )
}
