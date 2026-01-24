package com.yapp2app.photo.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * fileName       : UpdatePhotoFavoriteRequest
 * author         : koo
 * date           : 2026. 1. 14. 오전 3:16
 * description    :
 */
data class UpdatePhotoFavoriteRequest(
    @field:Schema(description = "변경하고자 하는 즐겨찾기 상태", example = "true")
    @field:NotNull(message = "favorite은 필수값입니다.")
    val favorite: Boolean?,
)
