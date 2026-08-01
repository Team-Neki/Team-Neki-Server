package com.neki.pose.entity

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
 * fileName       : ScrapPose
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Entity
@Table(name = "TB_SCRAP_POSE")
@EntityListeners(AuditingEntityListener::class)
class ScrapPose(
    @EmbeddedId
    val id: ScrapPoseId,

    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime? = null,
) {

    constructor(userId: Long, imageId: Long) : this(
        id = ScrapPoseId(
            userId = userId,
            poseId = imageId,
        ),
    )
}

@Embeddable
data class ScrapPoseId(
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "pose_id")
    val poseId: Long,
) : Serializable
