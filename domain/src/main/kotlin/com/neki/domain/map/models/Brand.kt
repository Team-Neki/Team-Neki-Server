package com.neki.domain.map.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : Brand
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 브랜드 엔티티
 */
@Entity
@Table(name = "TB_BRAND")
class Brand(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false, length = 50, unique = true)
    var name: String,

    @Column(name = "code", nullable = false, length = 30, unique = true)
    var code: String,

    @Column(name = "media_id", nullable = true)
    var mediaId: Long? = null,

    @Column(name = "supportAndroidQr", nullable = false)
    var supportAndroidQr: Boolean = false,

    @Column(name = "supportIosQr", nullable = false)
    var supportIosQr: Boolean = false,

    @Column(name = "exposeToMap", nullable = false)
    var exposeToMap: Boolean = false,

    @Column(name = "isDeleted", nullable = false)
    var isDeleted: Boolean = false,
) : BaseTimeEntity() {
    /**
     * null 인 인자는 변경하지 않는다.
     */
    fun updateInfo(
        name: String?,
        code: String?,
        supportAndroidQr: Boolean?,
        supportIosQr: Boolean?,
        exposeToMap: Boolean?,
    ) {
        name?.let { this.name = it }
        code?.let { this.code = it }
        supportAndroidQr?.let { this.supportAndroidQr = it }
        supportIosQr?.let { this.supportIosQr = it }
        exposeToMap?.let { this.exposeToMap = it }
    }

    fun softDelete() {
        isDeleted = true
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
