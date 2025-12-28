package com.yapp2app.photo.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * fileName       : Folder
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:24
 * description    : 사용자의 사진 폴더 엔티티. 한 사용자는 여러 폴더를 가질 수 있으며, 폴더는 여러 PhotoImage를 포함
 */
@Entity
@Table(name = "TB_folder")
class Folder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "cover_photo_id")
    var coverPhotoId: Long? = null,

    @OneToMany(mappedBy = "folder")
    val photoImages: MutableList<PhotoImage> = mutableListOf(),

) : BaseTimeEntity() {

    fun detachPhotos() {
        photoImages.forEach { it.detachFolder() }
        photoImages.clear()
    }
}
