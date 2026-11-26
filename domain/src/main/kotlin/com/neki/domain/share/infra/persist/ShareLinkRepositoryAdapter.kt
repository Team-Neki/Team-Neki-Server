package com.neki.domain.share.infra.persist

import com.neki.domain.share.infra.persist.jpa.JpaShareLinkRepository
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.repository.ShareLinkRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * fileName       : ShareLinkRepositoryAdapter
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : Share link Repository Adapter
 */
@Repository
class ShareLinkRepositoryAdapter(private val jpaRepository: JpaShareLinkRepository) : ShareLinkRepository {

    override fun getByToken(token: String): ShareLink? = jpaRepository.findByToken(token)

    override fun getLatestAlive(userId: Long, photoId: Long, now: LocalDateTime): ShareLink? =
        jpaRepository.findFirstByUserIdAndPhotoIdAndExpiresAtAfterOrderByExpiresAtDesc(userId, photoId, now)

    override fun save(shareLink: ShareLink): ShareLink = jpaRepository.save(shareLink)
}
