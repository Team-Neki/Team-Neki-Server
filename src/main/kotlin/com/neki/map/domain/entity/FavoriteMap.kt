package com.neki.map.domain.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/**
 * fileName       : FavoriteMap
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 지도(포토부스 위치) 즐겨찾기 엔티티
 */
@Entity
@Table(name = "TB_FAVORITE_MAP")
class FavoriteMap(

    @EmbeddedId
    val id: FavoriteMapId,
) : BaseTimeEntity() {

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
