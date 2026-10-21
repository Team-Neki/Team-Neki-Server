package com.neki.domain.map.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.LocalDateTime

/**
 * fileName       : FavoritePhotoBooth
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 즐겨찾기 (V40). 지점 마스터 id 대신 enriched 의 원천 키 (platform, idx) 로 부스를 가리킨다.
 *   기존 TB_FAVORITE_MAP(지점 마스터 id) 은 옮기지 않고 남겨 두며, 조회는 두 테이블을 합쳐서 본다 (MapService).
 */
@Entity
@Table(name = "TB_FAVORITE_PHOTO_BOOTH")
@EntityListeners(AuditingEntityListener::class)
class FavoritePhotoBooth(

    @EmbeddedId
    val id: FavoritePhotoBoothId,

    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime? = null,
) {

    constructor(userId: Long, key: PhotoBoothKey) : this(
        id = FavoritePhotoBoothId(
            userId = userId,
            platform = key.platform,
            idx = key.idx,
        ),
    )
}

@Embeddable
data class FavoritePhotoBoothId(

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false,
    )
    val userId: Long,

    @Column(
        name = "platform",
        nullable = false,
        updatable = false,
        length = 32,
    )
    val platform: String,

    @Column(
        name = "idx",
        nullable = false,
        updatable = false,
        length = 64,
    )
    val idx: String,

) : Serializable {
    fun key(): PhotoBoothKey = PhotoBoothKey(platform = platform, idx = idx)
}
