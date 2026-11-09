package com.neki.api.pose.api.dto

import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
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
            @field:Schema(
                description = "2인 포즈 관계 (FRIEND/COUPLE/SSUM). 관계 없는 포즈는 null",
                example = "FRIEND",
                nullable = true,
            )
            val relation: PoseRelation? = null,
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
        @field:Schema(
            description = "2인 포즈 관계 (FRIEND/COUPLE/SSUM). 관계 없는 포즈는 null",
            example = "FRIEND",
            nullable = true,
        )
        val relation: PoseRelation? = null,
    )

    @Schema(name = "BulkUploadPosesResponse")
    data class BulkUploadPoses(
        @field:Schema(description = "건수 요약")
        val summary: Summary,
        @field:Schema(description = "새로 등록한 포즈")
        val created: List<Created>,
        @field:Schema(description = "건너뛴 행 (ALREADY_REGISTERED, IMAGE_NOT_PROVIDED). 다시 보내도 같은 결과")
        val skipped: List<RowOutcome>,
        @field:Schema(
            description = "실패한 행 (INVALID_KEY, DUPLICATE_KEY_IN_CSV, INVALID_HEAD_COUNT, INVALID_RELATION, " +
                "RELATION_NOT_ALLOWED, DUPLICATE_IMAGE, IMAGE_SHARED_BY_ROWS, UNSUPPORTED_IMAGE, STORAGE_FAILED, SAVE_FAILED)",
        )
        val failed: List<RowOutcome>,
        @field:Schema(description = "어느 행에도 해당하지 않는 이미지 파일명")
        val unmatchedImages: List<String>,
    ) {
        @Schema(name = "BulkUploadPosesSummary")
        data class Summary(
            @field:Schema(description = "pose.csv 데이터 행 수", example = "153")
            val totalRows: Int,
            @field:Schema(description = "등록 수", example = "51")
            val created: Int,
            @field:Schema(description = "건너뜀 수", example = "102")
            val skipped: Int,
            @field:Schema(description = "실패 수", example = "0")
            val failed: Int,
            @field:Schema(description = "매칭되지 않은 이미지 수", example = "0")
            val unmatchedImages: Int,
        )

        @Schema(name = "BulkUploadPosesCreated")
        data class Created(
            @field:Schema(description = "pose.csv media_key", example = "friend_001_F1-4_F2-4")
            val sourceKey: String,
            @field:Schema(description = "포즈 ID", example = "1")
            val poseId: Long,
            @field:Schema(description = "미디어 ID", example = "1")
            val mediaId: Long,
            @field:Schema(description = "인원 수", example = "TWO")
            val headCount: HeadCount,
            @field:Schema(description = "관계", example = "FRIEND", nullable = true)
            val relation: PoseRelation?,
        )

        @Schema(name = "BulkUploadPosesRowOutcome")
        data class RowOutcome(
            @field:Schema(description = "pose.csv media_key", example = "couple_001_F1-4_M1-5")
            val sourceKey: String,
            @field:Schema(description = "pose.csv 줄 번호 (헤더가 1)", example = "2")
            val line: Int,
            @field:Schema(description = "사유", example = "IMAGE_NOT_PROVIDED")
            val reason: String,
        )
    }
}
