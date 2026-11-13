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
    var mediaId: Long,

    @Column(name = "head_count", nullable = false)
    @Enumerated(EnumType.STRING)
    val headCount: HeadCount,

    @Column(name = "memo", nullable = true)
    var memo: String? = null,

    // 2인 포즈에만 붙는다 (DB CHECK ck_pose_relation_two_only). 기존 포즈는 null
    @Column(name = "relation", nullable = true, length = 30)
    @Enumerated(EnumType.STRING)
    val relation: PoseRelation? = null,

    // 원본 키 (PM pose.csv 의 media_key). 관리자 등록(JSON /admin/upload)에서 선택으로 받으며 DB UNIQUE 라
    // 같은 키를 다시 등록하면 409 D-18 이다. 보내지 않은 포즈는 null.
    // V46 의 컬럼 COMMENT 는 v2 기준 문구지만 checksum 때문에 마이그레이션은 고치지 않는다
    @Column(name = "source_key", nullable = true, length = 100)
    val sourceKey: String? = null,
) : BaseTimeEntity() {
    fun changeMedia(mediaId: Long) {
        this.mediaId = mediaId
    }
}
