package com.neki.photo.domain.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "TB_PHOTO_IMAGE_FOLDER")
class PhotoImageFolder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "photo_image_id", nullable = false)
    val photoImageId: Long,

    @Column(name = "folder_id", nullable = true)
    val folderId: Long?,
) : BaseTimeEntity()
