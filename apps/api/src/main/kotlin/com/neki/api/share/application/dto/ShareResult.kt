package com.neki.api.share.application.dto

import com.neki.domain.share.models.SharedImageContent
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : ShareResult
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : Share application 결과
 */
object ShareResult {
    data class IssueShareLink(val token: String, val expiresAt: LocalDateTime)

    /**
     * content: 전달 방식별 결과 (Binary / Redirect)
     * maxAge: Cache-Control max-age (min(남은 시간, cacheMaxAge))
     * expiresAt: 링크 만료 일시 (X-Share-Expires-At)
     */
    data class ViewSharedImage(val content: SharedImageContent, val maxAge: Duration, val expiresAt: LocalDateTime)
}
