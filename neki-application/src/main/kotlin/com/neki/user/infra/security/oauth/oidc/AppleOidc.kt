package com.neki.user.infra.security.oauth.oidc

import com.neki.user.application.port.dto.AuthContract
import com.neki.user.enums.ProviderType
import com.neki.user.infra.security.config.OauthProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * fileName       : AppleOidc
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Apple OAuth 외부 연동 Adapter
 */
@Component
class AppleOidc(private val restClient: RestClient, private val oauthProperties: OauthProperties) : Oidc {
    override val providerType: ProviderType = ProviderType.APPLE

    override fun getOIDCPublicKey(): AuthContract.OIDCPublicKeysPayload = restClient.get()
        .uri(oauthProperties.apple.jwksUri)
        .retrieve()
        .body(AuthContract.OIDCPublicKeysPayload::class.java)!!
}
