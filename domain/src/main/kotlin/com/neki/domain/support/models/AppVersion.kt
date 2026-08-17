package com.neki.domain.support.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : AppVersion
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Entity
@Table(name = "TB_APP_VERSION")
class AppVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, unique = true)
    val platform: Platform,

    @Column(name = "min_version", nullable = false)
    var minVersion: String,

    @Column(name = "current_version", nullable = false)
    var currentVersion: String,
) : BaseTimeEntity() {

    fun updateVersions(minVersion: String, currentVersion: String) {
        this.minVersion = minVersion
        this.currentVersion = currentVersion
    }
}
