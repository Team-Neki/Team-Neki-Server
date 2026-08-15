package com.neki.domain.user.infra.security.oauth

/**
 * fileName       : AuthCacheKeys
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : Redis 캐시 키 중앙 관리
 *
 * 모든 캐시 키는 이 클래스에서 관리하여 일관성 유지
 * 포맷: {category}:{identifier}
 */
object AuthCacheKeys {
    const val KAKAO_OIDC_KEY = "oidcPublicKeys:kakao"
    const val APPLE_OIDC_KEY = "oidcPublicKeys:apple"
}
