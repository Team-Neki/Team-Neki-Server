package com.neki.user.application.contract

/**
 * fileName       : CacheKeys
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Redis 캐시 키 중앙 관리
 *
 * 모든 캐시 키는 이 클래스에서 관리하여 일관성 유지
 * 포맷: {category}:{identifier}
 */
object AuthCacheKeys {
    const val KAKAO_OIDC_KEY = "oidcPublicKeys:kakao"
    const val APPLE_OIDC_KEY = "oidcPublicKeys:apple"
}
