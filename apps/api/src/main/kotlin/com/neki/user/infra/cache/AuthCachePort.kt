package com.neki.user.infra.cache

import com.neki.user.infra.security.oauth.dto.OIDCPublicKeysPayload
import java.time.Duration

/**
 * fileName       : CachePort
 * author         : darren
 * date           : 2026. 1. 12. 10:18
 * description    : 캐시사용을 위한 Port 인터페이스
 */
interface AuthCachePort {
    fun setPublicKeys(key: String, value: OIDCPublicKeysPayload, ttl: Duration)

    fun getPublicKeys(key: String): OIDCPublicKeysPayload?

    fun clearPublicKeys(key: String)
}
