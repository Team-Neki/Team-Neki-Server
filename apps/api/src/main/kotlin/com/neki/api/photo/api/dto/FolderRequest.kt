package com.neki.api.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * fileName       : FolderRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 폴더 관련 요청 DTO
 */
object FolderRequest {
    @Schema(name = "CreateFolderRequest")
    data class CreateFolder(
        @field:Schema(description = "폴더명", example = "즐겨찾기")
        @field:NotBlank(message = "폴더명은 필수입니다.")
        @field:Size(min = 1, max = 10, message = "폴더명은 1자 이상 10자 이하여야 합니다.")
        val name: String?,
    )

    @Schema(name = "DeleteFoldersRequest")
    data class DeleteFolders(
        @field:Schema(
            description = "삭제할 폴더 ID 목록",
            example = "[1, 2, 3]",
        )
        @field:NotEmpty(message = "삭제할 폴더 ID 목록은 비어있을 수 없습니다.")
        val folderIds: List<Long>,
    )

    @Schema(name = "UpdateFolderRequest")
    data class UpdateFolder(
        @field:Schema(description = "변경할 폴더명", example = "대학교 친구")
        @field:NotBlank(message = "폴더명은 필수입니다.")
        @field:Size(min = 1, max = 10, message = "폴더명은 1자 이상 10자 이하여야 합니다.")
        val name: String?,
    )

    @Schema(name = "RemovePhotosFromFolderRequest")
    data class RemovePhotosFromFolder(
        @field:Schema(
            description = "폴더에서 제외할 사진 ID 목록",
            example = "[1, 2, 3]",
        )
        @field:NotEmpty(message = "제외할 사진 ID 목록은 비어있을 수 없습니다.")
        val photoIds: List<Long>,
    )

    @Schema(name = "MovePhotosToFolderRequest")
    data class MovePhotosToFolder(
        @field:Schema(
            description = "이동전 폴더 ID",
            example = "1",
        )
        @field:NotNull(message = "이동전 폴더 ID는 필수입니다.")
        val sourceFolderId: Long?,

        @field:Schema(
            description = "이동할 사진 ID 목록",
            example = "[1, 2, 3]",
        )
        @field:NotEmpty(message = "이동할 사진 ID 목록은 비어있을 수 없습니다.")
        val photoIds: List<Long>,

        @field:Schema(
            description = "이동 대상 폴더 ID 목록",
            example = "[1, 2, 3]",
        )
        val targetFolderIds: List<Long>,
    )

    @Schema(name = "CopyPhotosToFolderRequest")
    data class CopyPhotosToFolder(
        @field:Schema(
            description = "복제할 사진 ID 목록",
            example = "[1, 2, 3]",
        )
        @field:NotEmpty(message = "복제할 사진 ID 목록은 비어있을 수 없습니다.")
        val photoIds: List<Long>,

        @field:Schema(
            description = "이동 대상 폴더 ID 목록",
            example = "[1, 2, 3]",
        )
        @field:NotEmpty(message = "복제할 폴더 ID 목록은 비어있을 수 없습니다.")
        val targetFolderIds: List<Long>,
    )
}
