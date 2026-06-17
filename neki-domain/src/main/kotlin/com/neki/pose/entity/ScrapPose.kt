package com.neki.pose.entity

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
    protected constructor() : this(
        ScrapPoseId(0L, 0L),
    )

    constructor(userId: Long, imageId: Long) : this(
        ScrapPoseId(userId, imageId),
    )
}

@Embeddable
data class ScrapPoseId(
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "pose_id")
    val poseId: Long,
) : Serializable
