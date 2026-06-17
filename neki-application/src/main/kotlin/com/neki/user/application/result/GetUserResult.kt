package com.neki.user.application.result

import com.neki.user.enums.ProviderType

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
