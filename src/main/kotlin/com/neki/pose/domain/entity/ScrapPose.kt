package com.neki.pose.domain.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/**
 * fileName       : ScrapPose
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Entity
@Table(name = "TB_SCRAP_POSE")
class ScrapPose(
    @EmbeddedId
    val id: ScrapPoseId,
) : BaseTimeEntity() {

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
