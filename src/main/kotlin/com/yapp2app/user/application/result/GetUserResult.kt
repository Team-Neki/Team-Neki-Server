package com.yapp2app.user.application.result

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : GetUserInfoResult
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:27
 * description    :
 */
data class GetUserResult(
    val userId: Long,
    val name: String,
    val email: String?,
    val objectKey: String?,
    val providerType: ProviderType,
    val agreeTerms: Boolean,
)
