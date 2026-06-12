package com.neki.notification.domain.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : Notification
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 사용자별 알림 토큰 및 푸시 알림 동의 여부
 */
@Entity
@Table(name = "TB_NOTIFICATION")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "device_token", nullable = false, length = 512)
    var deviceToken: String,

    @Column(name = "push_agreed", nullable = false)
    var pushAgreed: Boolean = false,
) : BaseTimeEntity()
