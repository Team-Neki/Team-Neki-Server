package com.yapp2app.media.infra.storage.fake

import com.yapp2app.media.application.dto.MediaRef
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.domain.MediaType
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
    fun fakeMediaStorage(): MediaStoragePort = FakeMediaStorageAdapter()
}

class FakeMediaStorageAdapter : MediaStoragePort {

    private val storage = ConcurrentHashMap<String, ByteArray>()

    override fun deleteByKey(key: String) {
        storage.remove(key)
    }

    override fun findByKey(key: String): String = "https://fake-storage.test/$key"

    override fun fetchBinaryByKey(key: String): ByteArray = storage[key] ?: ByteArray(0)

    override fun findAll(prefix: String): List<MediaRef> = storage.keys
        .filter { it.startsWith(prefix) }
        .map { MediaRef(key = it, url = findByKey(it), type = MediaType.PHOTO_BOOTH) }

    override fun exists(key: String): Boolean = true

    override fun generateUploadTicket(key: String, contentType: String): MediaStoragePort.UploadTicket =
        MediaStoragePort.UploadTicket(
            url = "https://fake-storage.test/upload/$key",
            method = "PUT",
            expiresAt = Instant.now().plusSeconds(3600),
            contentType = contentType,
        )
}
