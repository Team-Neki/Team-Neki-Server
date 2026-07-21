package com.neki.user.application.port

import com.neki.user.application.port.dto.AuthContract
import java.time.Duration

/**
 * fileName       : CachePort
 * author         : darren
 * date           : 2026. 1. 12. 10:18
 * description    : 캐시사용을 위한 Port 인터페이스
 */
interface AuthCachePort {
    fun setPublicKeys(key: String, value: AuthContract.OIDCPublicKeysPayload, ttl: Duration)

    fun getPublicKeys(key: String): AuthContract.OIDCPublicKeysPayload?

    fun clearPublicKeys(key: String)
}
