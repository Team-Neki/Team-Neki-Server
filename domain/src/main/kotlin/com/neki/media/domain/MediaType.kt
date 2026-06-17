package com.neki.media.domain

import java.time.Duration

/**
 * fileName       : MediaType
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : 이미지 저장 type
 */
enum class MediaType(val prefix: String, val cacheTtl: Duration?) {

    /**
     * 사용자 프로필
     */
    USER_PROFILE("user-profiles", Duration.ofHours(24)),

    /**
     * 인생네컷
     */
    PHOTO_BOOTH("photo-booth", null),

    /**
     * 확장성을 고려한 첨부 이미지
     */
    ATTACHMENT("attachments", null),

    /**
     * 로고 이미지
     */
    LOGO("logo", Duration.ofHours(24)),

    /**
     * 포즈 이미지
     */
    POSE("pose", Duration.ofHours(24)),

    /**
     * 업로드 검증, 테스트 등
     */
    TEMP("temp", null),
    ;

    val isCacheable: Boolean get() = cacheTtl != null

    companion object {
        private val PREFIX_MAP = entries.associateBy { it.prefix }

        fun fromObjectKey(objectKey: String): MediaType? {
            val prefix = objectKey.substringBefore('/')
            return PREFIX_MAP[prefix]
        }
    }
}
