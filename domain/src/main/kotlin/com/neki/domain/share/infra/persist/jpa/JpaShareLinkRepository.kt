package com.neki.domain.share.infra.persist.jpa

import com.neki.domain.share.models.ShareLink
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

/**
 * fileName       : JpaShareLinkRepository
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : share link jpa repository
 */
interface JpaShareLinkRepository : JpaRepository<ShareLink, Long> {

    fun findByToken(token: String): ShareLink?

    fun findFirstByUserIdAndPhotoIdAndExpiresAtAfterOrderByExpiresAtDesc(
        userId: Long,
        photoId: Long,
        now: LocalDateTime,
    ): ShareLink?
}
