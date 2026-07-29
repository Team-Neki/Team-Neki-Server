package com.neki.user.api.dto

import com.neki.user.domain.enums.ProviderType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : UserResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User 관련 응답 DTO
 */
object UserResponse {
    @Schema(name = "GetUserResponse")
    data class GetUser(

        @field:Schema(description = "회원 ID", example = "1")
        val userId: Long,

        @field:Schema(description = "닉네임", example = "대현")
        val name: String,

        @field:Schema(description = "이메일", example = "yapp@neki.com")
        val email: String?,

        @field:Schema(description = "프로필 이미지 URL", example = "https://dev-yapp.suitestudy.com:4641/file/image/...")
        val profileImageUrl: String?,

        @field:Schema(description = "로그인 타입", example = "KAKAO")
        val providerType: ProviderType,

        @field:Schema(description = "최신 약관 동의 여부", example = "true")
        val agreeTerms: Boolean,

        @field:Schema(description = "마케팅 약관 동의 여부", example = "true")
        val marketingTerm: Boolean,

        @field:Schema(description = "푸시 알림 동의 여부", example = "false")
        val pushAgreed: Boolean,
    )
}
