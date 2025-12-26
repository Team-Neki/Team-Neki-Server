package com.yapp2app.common.media

/**
 * fileName       : MediaRef
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : |
 *   외부로 노출 가능한 미디어 리소스의 참조 정보
 *   - object key를 기준으로 식별
 *   - 접근 URL은 저장소 구현체를 통해 동적으로 생성
 */
data class MediaRef(
    val key: String, // key
    val url: String, // 접근 URL (signed or CDN)
    val type: MediaType,
)
