package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse
import com.yapp2app.auth.application.port.OidcPort
import com.yapp2app.auth.infra.security.properties.OauthProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * fileName       : KakaoOidcAdapter
 * author         : darren
 * date           : 2025. 12. 26. 18:20
 * description    : 카카오 OAuth 외부 연동 Adapter
 */
@Component
class KakaoOidcAdapter(private val restClient: RestClient, private val oauthProperties: OauthProperties) : OidcPort {

    override fun getOIDCPublicKey(): OIDCPublicKeysResponse = restClient.get()
        .uri(oauthProperties.kakao.jwksUri)
        .retrieve()
        .body(OIDCPublicKeysResponse::class.java)!!
}
