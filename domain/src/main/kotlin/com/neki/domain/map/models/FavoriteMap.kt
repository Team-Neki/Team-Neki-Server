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
 * fileName       : FavoriteMap
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 지도(포토부스 위치) 즐겨찾기 엔티티
 */
@Entity
@Table(name = "TB_FAVORITE_MAP")
@EntityListeners(AuditingEntityListener::class)
class FavoriteMap(

    @EmbeddedId
    val id: FavoriteMapId,

    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime? = null,
) {

    constructor(userId: Long, locationId: Long) : this(
        id = FavoriteMapId(
            userId = userId,
            locationId = locationId,
        ),
    )
}

@Embeddable
data class FavoriteMapId(

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false,
    )
    val userId: Long,

    @Column(
        name = "location_id",
        nullable = false,
        updatable = false,
    )
    val locationId: Long,

) : Serializable
