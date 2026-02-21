package com.neki.auth.infra.oauth.oidc

import com.neki.auth.application.contract.OIDCPublicKeysPayload
import com.neki.auth.infra.oauth.oidc.Oidc
import com.neki.auth.infra.security.properties.OauthProperties
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
