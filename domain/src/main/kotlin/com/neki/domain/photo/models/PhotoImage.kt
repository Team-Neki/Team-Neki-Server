package com.neki.domain.photo.models

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
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

/**
 * fileName       : PhotoImage
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:13
 * description    : 사용자의 사진 엔티티. url 대신 mediaId로 접근
 */
@Entity
@DynamicUpdate
@SQLRestriction("deleted_at IS NULL")
@Table(name = "TB_PHOTO_IMAGE")
class PhotoImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "media_id", nullable = false)
    val mediaId: Long,

    @Column(name = "memo", nullable = true)
    var memo: String? = null,

    @Column(name = "upload_method", nullable = true, length = 15)
    @Enumerated(EnumType.STRING)
    val uploadMethod: UploadMethod? = null,

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = null,

    @Column(name = "captured_at", nullable = true)
    var capturedAt: LocalDateTime? = null,

) : BaseTimeEntity() {

    fun softDelete() {
        deletedAt = LocalDateTime.now()
    }

    fun update(memo: String?, capturedAt: LocalDateTime?) {
        this.memo = memo
        this.capturedAt = capturedAt
    }

    fun updateMemo(memo: String) {
        this.memo = memo
    }
}
