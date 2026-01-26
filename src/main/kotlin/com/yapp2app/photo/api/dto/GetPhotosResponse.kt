package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : PhotoImageResponse
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain 응답
 */
data class GetPhotosResponse(
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
        @field:Schema(description = "폴더 ID", example = "1")
        val folderId: Long?,
        @field:Schema(description = "즐겨찾기 여부", example = "true")
        val favorite: Boolean,
        @field:Schema(description = "파일 형식", example = "image/jpeg")
        val contentType: String,
        @field:Schema(description = "업로드 날짜", example = "2025-12-23T07:09:00")
        val createdAt: String,
    )
}

data class GetFavoriteSummaryResponse(val latestImageUrl: String?, val totalCount: Long)
