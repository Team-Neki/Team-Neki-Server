package com.neki.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResponse
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain 응답
 */
data class GetPhotosResponse(
    @field:Schema(description = "전체 갯수")
    val totalCount: Long,
    @field:Schema(description = "사진 목록")
    val items: List<PhotoInfo>,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    data class PhotoInfo(
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

data class GetPhotoResponse(
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

data class GetFavoriteSummaryResponse(
    @field:Schema(description = "가장 최근 즐겨찾기 사진 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
    val latestImageUrl: String?,
    @field:Schema(description = "즐겨찾기 갯수", example = "1")
    val totalCount: Long,
)
