package com.neki.media.api.dto

import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * fileName       : FileConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : File api layer converter
 */
object FileConverter {
    @Component
    class RequestConverter {
        fun toGetImageByKeyQuery(objectKey: String): MediaQuery.GetImageByKey =
            MediaQuery.GetImageByKey(objectKey = objectKey)
    }

    @Component
    class ResponseConverter {
        fun toImageResponse(result: MediaResult.GetImageByKey): ResponseEntity<ByteArray> = ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType))
            .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS))
            .header(HttpHeaders.CONTENT_LENGTH, result.binaryData.size.toString())
            .body(result.binaryData)

        companion object {
            private const val CACHE_MAX_AGE_SECONDS = 86400L // 24시간
        }
    }
}
