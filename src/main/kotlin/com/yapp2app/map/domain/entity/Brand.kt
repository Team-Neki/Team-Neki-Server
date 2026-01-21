package com.yapp2app.map.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
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
    val name: String,

    @Column(name = "code", nullable = false, length = 30, unique = true)
    val code: String,

    @Column(name = "media_id", nullable = true)
    var mediaId: Long? = null,

) : BaseTimeEntity()
