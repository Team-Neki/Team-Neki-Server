package com.yapp2app.media.api.converter

import com.yapp2app.media.application.result.GetImageByKeyResult
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * fileName       : FileResultConverter
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : File api layer response 변환을 위한 converter
 */
@Component
class FileResultConverter {

    fun toImageResponse(result: GetImageByKeyResult): ResponseEntity<ByteArray> = ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(result.contentType))
        .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS))
        .header(HttpHeaders.CONTENT_LENGTH, result.binaryData.size.toString())
        .body(result.binaryData)

    companion object {
        private const val CACHE_MAX_AGE_SECONDS = 86400L // 24시간
    }
}
