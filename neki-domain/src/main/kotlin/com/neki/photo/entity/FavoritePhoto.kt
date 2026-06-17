package com.neki.photo.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/**
 * fileName       : FavoriteImage
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:20
 * description    :
 */
@Entity
@Table(name = "TB_FAVORITE_IMAGE")
class FavoritePhoto(

    @EmbeddedId
    val id: FavoritePhotoId,
) : BaseTimeEntity() {
    protected constructor() : this(
        FavoritePhotoId(0L, 0L),
    )

    constructor(userId: Long, imageId: Long) : this(
        FavoritePhotoId(userId, imageId),
    )
}

@Embeddable
data class FavoritePhotoId(

    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "image_id")
    val photoId: Long,

) : Serializable
