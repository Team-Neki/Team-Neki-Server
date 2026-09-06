package com.neki.domain.map.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

/**
 * fileName       : Brand
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 브랜드 엔티티
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(name = "TB_BRAND")
class Brand(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 유니크 제약은 partial index(uk_brand_name, uk_brand_code)로 살아있는 행에만 적용된다. V29 참조
    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "code", nullable = false, length = 30)
    var code: String,

    @Column(name = "media_id", nullable = true)
    var mediaId: Long? = null,

    @Column(name = "support_android_qr", nullable = false)
    var supportAndroidQr: Boolean = false,

    @Column(name = "support_ios_qr", nullable = false)
    var supportIosQr: Boolean = false,

    @Column(name = "expose_to_map", nullable = false)
    var exposeToMap: Boolean = false,

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
    /**
     * null 인 인자는 변경하지 않는다.
     */
    fun updateInfo(
        name: String?,
        code: String?,
        mediaId: Long?,
        supportAndroidQr: Boolean?,
        supportIosQr: Boolean?,
        exposeToMap: Boolean?,
    ) {
        name?.let { this.name = it }
        code?.let { this.code = it }
        mediaId?.let { this.mediaId = it }
        supportAndroidQr?.let { this.supportAndroidQr = it }
        supportIosQr?.let { this.supportIosQr = it }
        exposeToMap?.let { this.exposeToMap = it }
    }

    fun softDelete() {
        deletedAt = LocalDateTime.now()
    }

    companion object {
        fun of(name: String, code: String, mediaId: Long, supportAndroidQr: Boolean, supportIosQr: Boolean): Brand =
            Brand(
                name = name,
                code = code,
                mediaId = mediaId,
                supportAndroidQr = supportAndroidQr,
                supportIosQr = supportIosQr,
            )
    }
}
