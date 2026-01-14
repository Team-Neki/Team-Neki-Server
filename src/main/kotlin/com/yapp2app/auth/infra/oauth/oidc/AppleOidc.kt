package com.yapp2app.auth.infra.oauth.oidc

import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse
import com.yapp2app.auth.infra.oauth.oidc.Oidc
import com.yapp2app.auth.infra.security.properties.OauthProperties
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
    override fun getOIDCPublicKey(): OIDCPublicKeysResponse = restClient.get()
        .uri(oauthProperties.apple.jwksUri)
        .retrieve()
        .body(OIDCPublicKeysResponse::class.java)!!
}
