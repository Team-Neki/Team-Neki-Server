package com.neki.user.api.dto

import com.neki.user.domain.enums.ProviderType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : GetUserInfoResponse
 * author         : darren
 * date           : 2025. 12. 31. 14:45
 * description    :
 */

data class GetUserResponse(

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
)
