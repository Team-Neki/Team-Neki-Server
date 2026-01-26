package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : PhotoBoothResponse
 * author         : koo
 * date           : 2025. 12. 23. 오후 6:50
 * description    : Folder aggregate에 대한 응답
 */
data class CreateFolderResponse(
    @field:Schema(description = "폴더 ID", example = "1")
    val folderId: Long,
)

data class GetAllFolderResponse(
    @field:Schema(description = "폴더 목록")
    val items: List<FolderInfo>,
) {
    data class FolderInfo(
        @field:Schema(description = "폴더 ID", example = "1")
        val folderId: Long,
        @field:Schema(description = "폴더명", example = "즐겨찾기")
        val name: String,
    )
}
