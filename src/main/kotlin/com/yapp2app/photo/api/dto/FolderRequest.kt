package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * fileName       : FolderRequest
 * author         : koo
 * date           : 2025. 12. 23. 오후 10:27
 * description    : 폴더 관련 요청 body
 */
data class CreateFolderRequest(
    @field:Schema(description = "폴더명", example = "즐겨찾기")
    @field:NotBlank(message = "폴더명은 필수입니다.")
    @field:Size(min = 1, max = 10, message = "폴더명은 1자 이상 10자 이하여야 합니다.")
    val name: String?,
)

data class DeleteFoldersRequest(
    @field:Schema(
        description = "삭제할 폴더 ID 목록",
        example = "[1, 2, 3]",
    )
    @field:NotEmpty(message = "삭제할 폴더 ID 목록은 비어있을 수 없습니다.")
    val folderIds: List<Long>,
)

data class UpdateFolderRequest(
    @field:Schema(description = "변경할 폴더명", example = "대학교 친구")
    @field:NotBlank(message = "폴더명은 필수입니다.")
    @field:Size(min = 1, max = 10, message = "폴더명은 1자 이상 10자 이하여야 합니다.")
    val name: String?,
)

data class RemovePhotosFromFolderRequest(
    @field:Schema(
        description = "폴더에서 제외할 사진 ID 목록",
        example = "[1, 2, 3]",
    )
    @field:NotEmpty(message = "제외할 사진 ID 목록은 비어있을 수 없습니다.")
    val photoIds: List<Long>,
)
