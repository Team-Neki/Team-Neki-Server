package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * fileName       : FolderRequest
 * author         : koo
 * date           : 2025. 12. 23. 오후 10:27
 * description    : 폴더 관련 요청 body
 */
data class CreateFolderRequest(
    @field:Schema(description = "폴더명", example = "즐겨찾기")
    @field:NotBlank val name: String,
)

data class DeleteFoldersRequest(
    @field:Schema(
        description = "삭제할 폴더 ID 목록",
        example = "[1, 2, 3]",
    ) @field:NotEmpty val folderIds: List<Long>,
)

data class UpdateFolderRequest(
    @field:Schema(description = "변경할 폴더명", example = "대학교 친구")
    @field:NotBlank val name: String,
)
