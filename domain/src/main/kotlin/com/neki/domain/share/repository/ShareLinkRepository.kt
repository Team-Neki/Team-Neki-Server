package com.neki.domain.share.repository

import com.neki.domain.share.models.ShareLink
import java.time.LocalDateTime

/**
 * fileName       : ShareLinkRepository
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크 영속성 인터페이스
 */
interface ShareLinkRepository {

    /** soft delete 된 링크는 @SQLRestriction 으로 제외된다 */
    fun getByToken(token: String): ShareLink?

    /** expires_at > now 인 링크 중 만료가 가장 늦은 것 */
    fun getLatestAlive(userId: Long, photoId: Long, now: LocalDateTime): ShareLink?

    fun save(shareLink: ShareLink): ShareLink
}
