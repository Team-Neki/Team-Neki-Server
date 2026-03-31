package com.neki.user.infra.security.oauth.oidc

import com.neki.user.application.contract.AuthCacheKeys
import com.neki.user.application.contract.OIDCPublicKeysPayload
import com.neki.user.application.port.AuthCachePort
import com.neki.user.infra.security.config.OauthProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * fileName       : KakaoOidc
 * author         : darren
 * date           : 2025. 12. 26. 18:20
 * description    : 카카오 OAuth 외부 연동 Adapter
 *
 * OIDC 공개키 캐싱 전략:
 * - RedisCacheService를 통한 캐시 제어
 * - 카카오 측에서 요청 트래픽이 많으면 차단하므로 캐싱 필수
 * - 서명 검증 실패 시 UseCase에서 캐시 무효화 후 재조회 패턴 적용
 */
@Component
class KakaoOidc(
    private val restClient: RestClient,
    private val oauthProperties: OauthProperties,
    private val authRedisCacheAdapter: AuthCachePort,
) : Oidc {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 카카오 OIDC 공개키 조회
     * - 캐시 키: oidcPublicKeys:kakao
     * - TTL: 6시간
     * - 캐시 미스 시 카카오 API 호출 후 캐싱
     */
    override fun getOIDCPublicKey(): OIDCPublicKeysPayload {
        // 1. 캐시 조회
        val cached = authRedisCacheAdapter.getPublicKeys(AuthCacheKeys.KAKAO_OIDC_KEY)

        if (cached != null) {
            return cached
        }

        // 2. 캐시 미스: 카카오 API 호출
        val publicKeys = restClient.get()
            .uri(oauthProperties.kakao.jwksUri)
            .retrieve()
            .body(OIDCPublicKeysPayload::class.java)!!

        // 3. 캐시 저장 (TTL 14일)
        authRedisCacheAdapter.setPublicKeys(AuthCacheKeys.KAKAO_OIDC_KEY, publicKeys, Duration.ofDays(14))

        return publicKeys
    }
}
