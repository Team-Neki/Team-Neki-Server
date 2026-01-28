package com.yapp2app.pose.api.dto

import com.yapp2app.pose.domain.HeadCount
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : GetPosesResponse
 * author         : darren
 * date           : 2026. 1. 28. 11:24
 * description    : Pose domain 응답
 */
data class GetPosesResponse(
    @field:Schema(description = "포즈 목록")
    val items: List<PoseInfo>,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    data class PoseInfo(
        @field:Schema(description = "포즈 ID", example = "1")
        val poseId: Long,
        @field:Schema(description = "인원 수", example = "ONE")
        val headCount: HeadCount,
        @field:Schema(description = "사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
        val imageUrl: String,
        @field:Schema(description = "파일 형식", example = "image/jpeg")
        val contentType: String,
        @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
        val createdAt: LocalDateTime,
    )
}

data class GetPoseResponse(
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
    @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
    val createdAt: LocalDateTime,
)

data class GetRandomPoseResponse(
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
    @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
    val createdAt: LocalDateTime,
)
