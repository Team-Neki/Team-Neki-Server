package com.yapp2app.media.infra.client

import com.yapp2app.media.application.port.ExternalImageFetchPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

/**
 * fileName       : RestClientImageFetcher
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : RestClient를 사용한 외부 이미지 다운로드 Adapter
 */
@Component
class RestClientImageFetcher(private val restClient: RestClient) : ExternalImageFetchPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(url: String): ExternalImageFetchPort.FetchResult? = try {
        // URI.create()를 사용하여 이미 인코딩된 URL의 이중 인코딩 방지
        val response = restClient.get()
            .uri(URI.create(url))
            .retrieve()
            .toEntity(ByteArray::class.java)

        val binary = response.body ?: return null
        val contentType = response.headers.contentType?.toString() ?: DEFAULT_CONTENT_TYPE

        ExternalImageFetchPort.FetchResult(binary, contentType)
    } catch (e: Exception) {
        log.warn("Failed to fetch image from $url", e)
        null
    }

    companion object {
        private const val DEFAULT_CONTENT_TYPE = "image/jpeg"
    }
}
