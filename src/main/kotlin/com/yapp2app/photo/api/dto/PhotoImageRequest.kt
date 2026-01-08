package com.yapp2app.photo.api.dto

import jakarta.annotation.Nullable
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/**
 * fileName       : PhotoImageRequest
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:27
 * description    : Photo image domain 요청
 */
data class UploadPhotoRequest(
    @field:NotNull(message = "mediaId는 필수 입력값입니다.")
    val mediaId: Long?,

    @field:Nullable
    val folderId: Long?,
)

data class DeletePhotosRequest(
    @field:NotEmpty(message = "photoIds가 비어있습니다.")
    val photoIds: List<Long>,
)
