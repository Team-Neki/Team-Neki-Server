package com.yapp2app.media.domain

/**
 * fileName       : MediaType
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : 이미지 저장 type
 */
enum class MediaType(val prefix: String) {

    /**
     * 사용자 프로필
     */
    USER_PROFILE("user-profiles"),

    /**
     * 인생네컷
     */
    PHOTO_BOOTH("photo-booth"),

    /**
     * 확장성을 고려한 첨부 이미지
     */
    ATTACHMENT("attachments"),

    /**
     * 로고 이미지
     */
    LOGO("logo"),

    /**
     * 업로드 검증, 테스트 등
     */
    TEMP("temp"),
}
