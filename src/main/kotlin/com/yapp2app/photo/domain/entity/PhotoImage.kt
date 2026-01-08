package com.yapp2app.photo.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : PhotoImage
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:13
 * description    : 사용자의 사진 엔티티. url 대신 fileId로 접근
 */
@Entity
@Table(name = "TB_photo_image")
class PhotoImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "file_id", nullable = false, length = 64, unique = true)
    val fileId: String,

    @Column(name = "folder_id", nullable = true)
    var folderId: Long? = null,
) : BaseTimeEntity()
