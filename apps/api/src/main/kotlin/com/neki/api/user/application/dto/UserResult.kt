package com.neki.api.user.application.dto

import com.neki.domain.user.models.ProviderType

/**
 * fileName       : UserResult
 * author         : koo
 * date           : 2026. 8. 3. 오전 2:19
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
