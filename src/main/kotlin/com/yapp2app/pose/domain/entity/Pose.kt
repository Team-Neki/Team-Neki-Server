package com.yapp2app.pose.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import com.yapp2app.pose.domain.HeadCount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate

/**
 * fileName       : Pose
 * author         : darren
 * date           : 2026. 1. 27. 14:10
 * description    :
 */
@Entity
@DynamicUpdate
@Table(name = "TB_POSE")
class Pose(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long? = null,

    @Column(name = "media_id", nullable = false)
    val mediaId: Long,

    @Column(name = "head_count", nullable = false)
    @Enumerated(EnumType.STRING)
    val headCount: HeadCount,

    @Column(name = "memo", nullable = true)
    var memo: String? = null,
) : BaseTimeEntity()
