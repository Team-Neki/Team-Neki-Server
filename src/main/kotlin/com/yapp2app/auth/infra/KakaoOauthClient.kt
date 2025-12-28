package com.yapp2app.auth.infra

import com.yapp2app.auth.application.result.OIDCPublicKeysResult
import com.yapp2app.auth.infra.security.properties.OauthProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * fileName       : KakaoOauthClient
 * author         : darren
 * date           : 2025. 12. 26. 18:20
 * description    : Auth usercase 관련 result
 */
@Component
class KakaoOauthClient(private val restClient: RestClient, private val oauthProperties: OauthProperties) {

    fun getOIDCPublicKey(): OIDCPublicKeysResult = restClient.get()
        .uri(oauthProperties.kakao.jwksUri)
        .retrieve()
        .body(OIDCPublicKeysResult::class.java)!!
}
