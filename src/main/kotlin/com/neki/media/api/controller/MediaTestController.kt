package com.neki.media.api.controller

import com.neki.media.application.contract.UploadTicket
import com.neki.media.application.dto.MediaRef
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.domain.MediaKey
import com.neki.media.domain.MediaType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * fileName       : MediaTestController
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:58
 * description    : local 환경 이미지 업로드 test
 */
@Profile("local")
@RestController
@RequestMapping("/api/media/test")
class MediaTestController(private val mediaStorage: MediaStoragePort) {

    @GetMapping
    fun listMedia(@RequestParam(defaultValue = "temp/") prefix: String): MediaListResponse {
        val mediaList: List<MediaRef> = mediaStorage.findAll(prefix)
        return MediaListResponse(
            prefix = prefix,
            count = mediaList.size,
            items = mediaList.map {
                MediaItem(
                    key = it.key,
                    url = it.url,
                    type = it.type.name,
                )
            },
        )
    }

    @PostMapping("/presigned")
    fun generatePresignedUrl(
        @RequestParam(required = false) filename: String?,
        @RequestParam(defaultValue = "image/jpeg") contentType: String,
    ): PresignedUrlResponse {
        // filename이 없는 경우 contentType에서 확장자 추출하여 기본 파일명 생성
        val effectiveFilename = filename ?: "upload.${contentType.substringAfter("/", "jpg")}"

        val key: String = MediaKey.generate(MediaType.TEMP, effectiveFilename, contentType)

        // Presigned URL 생성
        val uploadTicket: UploadTicket = mediaStorage.generateUploadTicket(
            key = key,
            contentType = contentType,
        )

        return PresignedUrlResponse(
            key = key,
            presignedUrl = uploadTicket.url,
            method = uploadTicket.method,
            expiresAt = uploadTicket.expiresAt,
            contentType = contentType,
        )
    }

    @GetMapping("/object/**")
    fun getMediaUrl(request: HttpServletRequest): MediaUrlResponse {
        // /api/media/test/object/ 이후의 전체 경로를 key로 사용
        val fullPath = request.requestURI
        val key = fullPath.removePrefix("/api/media/test/object/")
        val url: String = mediaStorage.findByKey(key)

        return MediaUrlResponse(
            key = key,
            url = url,
        )
    }

    @DeleteMapping("/object/**")
    fun deleteMedia(request: HttpServletRequest): ResponseEntity<MediaDeleteResponse> {
        // /api/media/test/object/ 이후의 전체 경로를 key로 사용
        val fullPath = request.requestURI
        val key = fullPath.removePrefix("/api/media/test/object/")

        mediaStorage.deleteByKey(key)

        return ResponseEntity.ok(
            MediaDeleteResponse(
                key = key,
                deleted = true,
            ),
        )
    }
}

data class MediaUrlResponse(val key: String, val url: String)

data class MediaDeleteResponse(val key: String, val deleted: Boolean)

data class MediaListResponse(val prefix: String, val count: Int, val items: List<MediaItem>)

data class MediaItem(val key: String, val url: String, val type: String)

data class PresignedUrlResponse(
    val key: String,
    val presignedUrl: String,
    val method: String,
    val expiresAt: Instant,
    val contentType: String,
)
