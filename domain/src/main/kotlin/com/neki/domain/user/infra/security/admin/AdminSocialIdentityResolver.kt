package com.neki.domain.user.infra.security.admin

import com.neki.domain.user.models.AdminIdentity
import com.neki.domain.user.service.AdminAccountAccess
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class AdminSocialIdentityResolver(
    private val accounts: AdminAccountAccess,
    private val providers: List<AdminIdentityProvider>,
) {
    fun resolve(registrationId: String, user: OAuth2User): Pair<Long, String> {
        val provider: AdminIdentityProvider = providers.singleOrNull { it.registrationId == registrationId }
            ?: throw OAuth2AuthenticationException(OAuth2Error("unsupported_provider"))
        val profile: AdminSocialProfile = provider.extract(user)
        val identityKey = "$registrationId:${profile.subject}"
        if (registrationId.isBlank() || profile.subject.isBlank() || identityKey.length > 512 ||
            profile.email.isBlank() || profile.email.length > 255
        ) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        val identity = AdminIdentity(key = identityKey, email = profile.email, name = profile.name)
        return accounts.findOrCreate(identity) to identity.key
    }
}
