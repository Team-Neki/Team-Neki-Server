package com.yapp2app.media.infra.cache

import com.yapp2app.media.application.port.MediaBinaryCachePort
import org.springframework.stereotype.Component

/**
 * fileName       : FakeMediaBinaryAdapter
 * author         : koo
 * date           : 2026. 1. 8. 오후 4:20
 * description    : 항상 cache miss가 발생하는 cache adapter
 */
@Component
class FakeMediaBinaryCacheAdapter : MediaBinaryCachePort {
    override fun get(key: String): ByteArray? {
        // 캐시 미사용: 항상 miss
        return null
    }

    override fun put(key: String, value: ByteArray) {
        // 캐시 미사용: 아무 동작도 하지 않음
    }

    override fun evict(key: String) {
        // 캐시 미사용: 항상 evict skip
    }
}
