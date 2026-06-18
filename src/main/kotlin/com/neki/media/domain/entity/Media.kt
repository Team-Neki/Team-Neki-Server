package com.neki.media.domain.entity

import com.neki.common.domain.BaseTimeEntity
import com.neki.media.domain.MediaType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : Media
 * author         : koo
 * date           : 2026. 1. 2.
 * description    : 미디어 파일 엔티티. fileId로 접근
 */
@Entity
@Table(name = "TB_MEDIA")
class Media(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "storage_key", nullable = false, length = 255)
    val storageKey: String,

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Column(name = "media_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val mediaType: MediaType,

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: MediaStatus = MediaStatus.INITIATED,

    @Column(name = "content_type", nullable = true, length = 100)
    val contentType: String,

    @Column(name = "width", nullable = true)
    var width: Int? = null,

    @Column(name = "height", nullable = true)
    var height: Int? = null,

    @Column(name = "size", nullable = true)
    var size: Long? = null,
) : BaseTimeEntity() {

    /**
     * width/height/size 백필용. 이미 값이 있는 필드는 덮어쓰지 않고 null 인 필드만 채운다.
     */
    fun backfillDimensions(width: Int?, height: Int?, size: Long?) {
        if (this.width == null) this.width = width
        if (this.height == null) this.height = height
        if (this.size == null) this.size = size
    }

    fun markAsUploaded() {
        this.status = MediaStatus.UPLOADED
    }

    fun markAsFailed() {
        this.status = MediaStatus.FAILED
    }

    fun markAsDeleteRequested() {
        this.status = MediaStatus.DELETE_REQUESTED
    }

    fun markAsDeleted() {
        this.status = MediaStatus.DELETED
    }

    fun markAsInitiated() {
        this.status = MediaStatus.INITIATED
    }

    fun isUploaded(): Boolean = status == MediaStatus.UPLOADED
}

enum class MediaStatus {
    INITIATED, // ticket 발급됨(업로드 시작 전)
    UPLOADED, // 업로드 완료 콜백/완료 API 호출됨
    FAILED, // 실패
    DELETE_REQUESTED, // 삭제 요청됨
    DELETED, // 삭제 완료
}
