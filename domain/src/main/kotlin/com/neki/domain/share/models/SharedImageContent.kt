package com.neki.domain.share.models

/**
 * fileName       : SharedImageContent
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 이미지 전달 결과. 서버 프록시는 Binary, CloudFront signed URL 은 Redirect
 */
sealed interface SharedImageContent {

    class Binary(val bytes: ByteArray, val contentType: String) : SharedImageContent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Binary) return false
            return bytes.contentEquals(other.bytes) && contentType == other.contentType
        }

        override fun hashCode(): Int = 31 * bytes.contentHashCode() + contentType.hashCode()
    }

    data class Redirect(val location: String) : SharedImageContent
}
