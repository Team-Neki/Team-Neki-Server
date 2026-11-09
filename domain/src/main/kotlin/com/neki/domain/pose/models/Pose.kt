package com.neki.domain.pose.models

import com.neki.core.domain.BaseTimeEntity
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

    // 관리자 업로드는 귀속 사용자가 없어 null 이다
    @Column(name = "user_id", nullable = true)
    val userId: Long? = null,

    @Column(name = "media_id", nullable = false)
    val mediaId: Long,

    @Column(name = "head_count", nullable = false)
    @Enumerated(EnumType.STRING)
    val headCount: HeadCount,

    @Column(name = "memo", nullable = true)
    var memo: String? = null,

    // 2인 포즈에만 붙는다 (DB CHECK ck_pose_relation_two_only). 기존 포즈는 null
    @Column(name = "relation", nullable = true, length = 30)
    @Enumerated(EnumType.STRING)
    val relation: PoseRelation? = null,

    // PM 원본 키 (pose.csv media_key). 일괄 업로드 멱등 판단용이며 DB UNIQUE 다. 수동 업로드는 null
    @Column(name = "source_key", nullable = true, length = 100)
    val sourceKey: String? = null,
) : BaseTimeEntity()
