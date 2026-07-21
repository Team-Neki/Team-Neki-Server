package com.neki.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * fileName       : UserRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User 관련 요청 DTO
 */
object UserRequest {
    @Schema(name = "UpdateUserRequest")
    data class UpdateUser(
        @field:Schema(description = "공백을 포함해 10글자 이하로 변경할 닉네임을 설정합니다", example = "새로운닉네임")
        @field:Size(max = 10, message = "닉네임은 공백 포함 10자 이하여야 합니다.")
        @field:NotBlank(message = "닉네임은 공백일 수 없습니다")
        val name: String,
    )

    @Schema(name = "UpdateUserProfileImageRequest")
    data class UpdateUserProfileImage(
        @field:Schema(description = "프로필 이미지로 설정할 mediaId, null일 경우 기본 이미지로 변경")
        val mediaId: Long? = null,
    )
}
