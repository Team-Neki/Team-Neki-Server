package com.neki.auth.application.port

import com.neki.auth.application.contract.OIDCPublicKeysResponse
import java.time.Duration

/**
 * fileName       : CachePort
 * author         : darren
 * date           : 2026. 1. 12. 10:18
 * description    : 캐시사용을 위한 Port 인터페이스
 */
interface AuthCachePort {
    fun setPublicKeys(key: String, value: OIDCPublicKeysResponse, ttl: Duration)

    fun getPublicKeys(key: String): OIDCPublicKeysResponse?

    fun clearPublicKeys(key: String)
}
