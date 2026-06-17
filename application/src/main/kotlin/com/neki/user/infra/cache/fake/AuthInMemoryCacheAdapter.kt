package com.neki.user.infra.cache.fake

import com.neki.user.application.contract.OIDCPublicKeysPayload
import com.neki.user.application.port.AuthCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * fileName       : AuthInMemoryCacheAdapter
 * author         : koo
 * date           : 2026. 3. 15. 오후 9:39
 * description    : staging/local 전용 캐시 어댑터
 */
@Profile("!prod")
@Component
class AuthInMemoryCacheAdapter : AuthCachePort {

    private val cache = mutableMapOf<String, Any>()
    override fun setPublicKeys(key: String, value: OIDCPublicKeysPayload, ttl: Duration) {
        try {
            cache[key] = value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getPublicKeys(key: String): OIDCPublicKeysPayload? = cache[key] as? OIDCPublicKeysPayload

    override fun clearPublicKeys(key: String) {
        cache.remove(key)
    }
}
