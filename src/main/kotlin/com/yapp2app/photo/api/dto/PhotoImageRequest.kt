package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.Nullable
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * fileName       : PhotoImageRequest
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:27
 * description    : Photo image domain 요청
 */
data class UploadPhotoRequest(
    @field:Nullable
    val folderId: Long?,

    @field:NotEmpty(message = "uploads가 비어있습니다.")
    @field:Valid
    @field:Size(max = 10, message = "한 번에 최대 10장까지 업로드할 수 있습니다.")
    val uploads: List<UploadPhotoItem>,

    @field:Schema(description = "업로드 사진 즐겨찾기 등록 여부", example = "true")
    val favorite: Boolean? = null,
) {
    data class UploadPhotoItem(
        @field:NotNull(message = "mediaId는 필수 입력값입니다.")
        val mediaId: Long?,

        val memo: String?,
    )
}

data class DeletePhotosRequest(
    @field:NotEmpty(message = "photoIds가 비어있습니다.")
    val photoIds: List<Long>,
)

data class UpdatePhotoRequest(val memo: String?)

data class UpdatePhotoFavoriteRequest(
    @field:Schema(description = "변경하고자 하는 즐겨찾기 상태", example = "true")
    @field:NotNull(message = "favorite은 필수값입니다.")
    val favorite: Boolean?,
)
