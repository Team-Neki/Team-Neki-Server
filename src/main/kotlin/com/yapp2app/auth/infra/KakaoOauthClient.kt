package com.yapp2app.auth.infra

import com.yapp2app.auth.infra.response.OIDCPublicKeysResponse
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

    // TODO Redis 연결 시 1주일간 캐싱 처리 필요 (카카오 측에서 요청 트레픽이 많으면 차단하기 떄문)
    fun getOIDCPublicKey(): OIDCPublicKeysResponse = restClient.get()
        .uri(oauthProperties.kakao.jwksUri)
        .retrieve()
        .body(OIDCPublicKeysResponse::class.java)!!
}
