package com.neki.domain.share.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * fileName       : ShareLink
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 사진 공유 링크. 공유 URL 에는 object key 대신 token 만 노출한다.
 * - token unique 는 partial unique index(V48)로만 건다. 엔티티에 unique = true 를 두지 않는다.
 */
@Entity
@DynamicUpdate
@SQLRestriction("deleted_at IS NULL")
@Table(name = "TB_SHARE_LINK")
class ShareLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "token", nullable = false, length = 64, updatable = false)
    val token: String,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(name = "photo_id", nullable = false, updatable = false)
    val photoId: Long,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    fun isExpired(now: LocalDateTime): Boolean = !now.isBefore(expiresAt)

    fun remaining(now: LocalDateTime): Duration =
        if (isExpired(now)) Duration.ZERO else Duration.between(now, expiresAt)

    companion object {
        /** expiresAt 은 초 단위로 자른다. X-Share-Expires-At 헤더 포맷과 DB/메모리 값을 맞추기 위함 */
        fun issue(token: String, userId: Long, photoId: Long, now: LocalDateTime, validFor: Duration): ShareLink =
            ShareLink(
                token = token,
                userId = userId,
                photoId = photoId,
                expiresAt = now.plus(validFor).truncatedTo(ChronoUnit.SECONDS),
            )
    }
}
