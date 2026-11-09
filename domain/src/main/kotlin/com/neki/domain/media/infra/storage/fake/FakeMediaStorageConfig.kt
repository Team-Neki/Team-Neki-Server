package com.neki.domain.media.infra.storage.fake

import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.MediaRef
import com.neki.domain.media.models.MediaStorageUploadTicket
import com.neki.domain.media.models.MediaType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * fileName       : FakeMediaStorageConfig
 * author         : koo
 * date           : 2026. 1. 8. 오후 6:32
 * description    : test를 위한 Media Storage Config
 */
@Profile("test")
@Configuration
class FakeMediaStorageConfig {

    @Bean
    fun fakeMediaStorage(): MediaStorage = FakeMediaStorageAdapter()
}

class FakeMediaStorageAdapter : MediaStorage {

    private val storage = ConcurrentHashMap<String, ByteArray>()
    private val fetchCount = ConcurrentHashMap<String, Int>()
    private var onFetchCallback: (() -> Unit)? = null
    private var putFailurePredicate: ((String) -> Boolean)? = null

    override fun deleteByKey(key: String) {
        storage.remove(key)
    }

    override fun findByKey(key: String): String = "https://fake-storage.test/$key"

    override fun fetchBinaryByKey(key: String): ByteArray {
        fetchCount.compute(key) { _, count -> (count ?: 0) + 1 }
        onFetchCallback?.invoke()
        return storage[key] ?: ByteArray(0)
    }

    override fun findAll(prefix: String): List<MediaRef> = storage.keys
        .filter { it.startsWith(prefix) }
        .map { MediaRef(key = it, url = findByKey(it), type = MediaType.PHOTO_BOOTH) }

    override fun exists(key: String): Boolean = true

    override fun generateUploadTicket(key: String, contentType: String): MediaStorageUploadTicket =
        MediaStorageUploadTicket(
            url = "https://fake-storage.test/upload/$key",
            method = "PUT",
            expiresAt = Instant.now().plusSeconds(3600),
            contentType = contentType,
        )

    override fun put(key: String, contentType: String, content: ByteArray) {
        if (putFailurePredicate?.invoke(key) == true) {
            throw IllegalStateException("fake storage put failure: $key")
        }
        storage[key] = content
    }

    // 테스트용 메서드
    fun putTestData(key: String, data: ByteArray) {
        storage[key] = data
    }

    fun clearTestData() {
        storage.clear()
        fetchCount.clear()
        onFetchCallback = null
        putFailurePredicate = null
    }

    /**
     * 조건에 맞는 key 의 put 을 실패시킨다. [clearTestData] 가 초기화한다.
     */
    fun failPutWhen(predicate: (String) -> Boolean) {
        putFailurePredicate = predicate
    }

    fun hasKey(key: String): Boolean = storage.containsKey(key)

    fun keys(): Set<String> = storage.keys.toSet()

    fun getFetchCount(key: String): Int = fetchCount[key] ?: 0

    fun getTotalFetchCount(): Int = fetchCount.values.sum()

    fun onFetch(callback: () -> Unit) {
        onFetchCallback = callback
    }
}
