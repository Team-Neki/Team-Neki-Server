package com.neki.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image 관련 응답 DTO
 */
object PhotoImageResponse {
    @Schema(name = "GetPhotosResponse")
    data class GetPhotos(
        @field:Schema(description = "전체 개수")
        val totalCount: Long,
        @field:Schema(description = "사진 목록")
        val items: List<Item>,
        @field:Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNext: Boolean,
    ) {
        @Schema(name = "PhotoInfo")
        data class Item(
            @field:Schema(description = "사진 ID", example = "1")
            val photoId: Long,
            @field:Schema(description = "사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
            val imageUrl: String,
            @field:Schema(description = "즐겨찾기 여부", example = "true")
            val favorite: Boolean,
            @field:Schema(description = "파일 형식", example = "image/jpeg")
            val contentType: String,
            @field:Schema(description = "이미지 너비", example = "1080", nullable = true)
            val width: Int? = null,
            @field:Schema(description = "이미지 높이", example = "1440", nullable = true)
            val height: Int? = null,
            @field:Schema(description = "메모", example = "친구들이랑 함께", nullable = true)
            val memo: String? = null,
            @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
            val createdAt: LocalDateTime,
        )
    }

    @Schema(name = "GetPhotoResponse")
    data class GetPhoto(
        @field:Schema(description = "사진 ID", example = "1")
        val photoId: Long,
        @field:Schema(description = "사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
        val imageUrl: String,
        @field:Schema(description = "즐겨찾기 여부", example = "true")
        val favorite: Boolean,
        @field:Schema(description = "파일 형식", example = "image/jpeg")
        val contentType: String,
        @field:Schema(description = "이미지 너비", example = "1080", nullable = true)
        val width: Int? = null,
        @field:Schema(description = "이미지 높이", example = "1440", nullable = true)
        val height: Int? = null,
        @field:Schema(description = "메모", example = "친구들이랑 함께", nullable = true)
        val memo: String? = null,
        @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
        val createdAt: LocalDateTime,
    )

    @Schema(name = "GetFavoriteSummaryResponse")
    data class GetFavoriteSummary(
        @field:Schema(
            description = "가장 최근 즐겨찾기 사진 URL",
            example = "https://dev-yapp.suitestudy.com:4641/file/image/...",
        )
        val latestImageUrl: String?,
        @field:Schema(description = "즐겨찾기 갯수", example = "1")
        val totalCount: Long,
    )
}
