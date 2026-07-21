package com.neki.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : FolderResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 폴더 관련 응답 DTO
 */
object FolderResponse {
    @Schema(name = "CreateFolderResponse")
    data class CreateFolder(
        @field:Schema(description = "폴더 ID", example = "1")
        val folderId: Long,
    )

    @Schema(name = "GetAllFolderResponse")
    data class GetAllFolder(
        @field:Schema(description = "폴더 목록")
        val items: List<FolderInfo>,
    ) {
        data class FolderInfo(
            @field:Schema(description = "폴더 ID", example = "1")
            val folderId: Long,
            @field:Schema(description = "폴더명", example = "즐겨찾기")
            val name: String,
            @field:Schema(
                description = "가장 최근 추가한 이미지",
                example = "https://dev-yapp.suitestudy.com:4641/file/image/...",
            )
            val latestImageUrl: String?,
            @field:Schema(description = "사진 개수", example = "10")
            val totalCount: Long,
        )
    }
}
