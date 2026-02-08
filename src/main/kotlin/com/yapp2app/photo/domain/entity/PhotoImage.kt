package com.yapp2app.photo.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate

/**
 * fileName       : PhotoImage
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:13
 * description    : 사용자의 사진 엔티티. url 대신 mediaId로 접근
 */
@Entity
@DynamicUpdate
@Table(name = "TB_PHOTO_IMAGE")
class PhotoImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "media_id", nullable = false)
    val mediaId: Long,

    @Column(name = "folder_id", nullable = true)
    var folderId: Long? = null,

    @Column(name = "memo", nullable = true)
    var memo: String? = null,
) : BaseTimeEntity()
