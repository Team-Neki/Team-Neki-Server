package com.yapp2app.user.api.dto

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : GetUserInfoResponse
 * author         : darren
 * date           : 2025. 12. 31. 14:45
 * description    :
 */

data class GetUserResponse(
    val userId: Long,
    val name: String,
    val email: String?,
    val profileImageUrl: String?,
    val providerType: ProviderType,
)
