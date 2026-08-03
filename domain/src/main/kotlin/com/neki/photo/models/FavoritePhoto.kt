package com.neki.photo.models

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
 * fileName       : FavoriteImage
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:20
 * description    :
 */
@Entity
@Table(name = "TB_FAVORITE_IMAGE")
@EntityListeners(AuditingEntityListener::class)
class FavoritePhoto(

    @EmbeddedId
    val id: FavoritePhotoId,

    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime? = null,
) {

    constructor(userId: Long, imageId: Long) : this(
        id = FavoritePhotoId(
            userId = userId,
            photoId = imageId,
        ),
    )
}

@Embeddable
data class FavoritePhotoId(

    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "image_id")
    val photoId: Long,

) : Serializable
