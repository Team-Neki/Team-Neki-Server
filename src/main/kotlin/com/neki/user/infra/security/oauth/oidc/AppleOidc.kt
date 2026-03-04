package com.neki.user.infra.security.oauth.oidc

import com.neki.user.contract.OIDCPublicKeysPayload
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
    override fun getOIDCPublicKey(): OIDCPublicKeysPayload = restClient.get()
        .uri(oauthProperties.apple.jwksUri)
        .retrieve()
        .body(OIDCPublicKeysPayload::class.java)!!
}
