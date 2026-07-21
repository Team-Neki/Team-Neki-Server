package com.neki.user.application.dto

import com.neki.user.domain.enums.ProviderType

/**
 * fileName       : UserResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain result
 */
object UserResult {
    data class GetUser(
        val userId: Long,
        val name: String,
        val email: String?,
        val objectKey: String?,
        val providerType: ProviderType,
        val agreeTerms: Boolean,
        val marketingTerm: Boolean,
        val pushAgreed: Boolean,
    )
}
