package com.neki.notification.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * fileName       : NotificationHist
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 발송된 푸시 알림 내역 (최근 알림 조회용)
 */
@Entity
@Table(name = "TB_NOTIFICATION_HIST")
class NotificationHist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "type", nullable = false, length = 50)
    val type: String,

    @Column(name = "title", nullable = false, length = 100)
    val title: String,

    @Column(name = "body", nullable = false, length = 500)
    val body: String,

    @Column(name = "link", length = 512)
    val link: String? = null,
) : BaseTimeEntity()
