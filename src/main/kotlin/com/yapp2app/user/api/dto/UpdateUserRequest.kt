package com.yapp2app.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * fileName       : UpdateUserRequest
 * author         : koo
 * date           : 2026. 1. 28. 오후 1:06
 * description    :
 */
data class UpdateUserRequest(
    @field:Schema(description = "프로필 이미지로 설정할 mediaId")
    val mediaId: Long? = null,

    @field:Schema(description = "공백을 포함해 10글자 이하로 변경할 닉네임을 설정합니다", example = "새로운닉네임")
    @field:Size(max = 10, message = "닉네임은 공백 포함 10자 이하여야 합니다.")
    val name: String? = null,
)
