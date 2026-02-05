package com.yapp2app.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * fileName       : UserRequest
 * author         : koo
 * date           : 2026. 1. 30. 오후 11:58
 * description    :
 */
data class UpdateUserRequest(
    @field:Schema(description = "공백을 포함해 12글자 이하로 변경할 닉네임을 설정합니다", example = "새로운닉네임")
    @field:Size(max = 12, message = "닉네임은 공백 포함 12자 이하여야 합니다.")
    @field:NotBlank(message = "닉네임은 공백일 수 없습니다")
    val name: String,
)

data class UpdateUserProfileImageRequest(
    @field:Schema(description = "프로필 이미지로 설정할 mediaId, null일 경우 기본 이미지로 변경")
    val mediaId: Long? = null,
)
