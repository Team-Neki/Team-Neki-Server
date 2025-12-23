package com.yapp2app.photobooth.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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

    @JoinColumn(name = "folder_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var folder: Folder? = null,

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: UploadStatus = UploadStatus.INITIATED,
) : BaseTimeEntity() {

    fun detachFolder() {
        this.folder = null
    }
}

enum class UploadStatus {
    INITIATED, // presigned url 발급됨(업로드 시작 전)
    UPLOADED, // 업로드 완료 콜백/완료 API 호출됨
    VERIFIED, // (선택) HEAD 요청 등으로 실제 존재 확인 완료
    FAILED, // 실패
}
