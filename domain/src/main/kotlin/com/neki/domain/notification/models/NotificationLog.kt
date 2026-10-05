package com.neki.domain.notification.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

/**
 * fileName       : NotificationLog
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 배치 발송 이력 (notification_log, V35). 전 결과를 남기며 (user_id, type, business_date) 가 중복 방지 키다.
 *                  created_at/updated_at 이 없는 테이블이라 BaseTimeEntity 를 상속하지 않는다
 */
@Entity
@Table(
    name = "notification_log",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_notification_log_user_type_date",
            columnNames = ["user_id", "notification_type", "business_date"],
        ),
    ],
    indexes = [Index(name = "ix_notification_log_business_date", columnList = "business_date")],
)
class NotificationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    val notificationType: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_tone", nullable = false, length = 16)
    val messageTone: MessageTone,

    @Column(name = "variable_applied", nullable = false)
    val variableApplied: Boolean,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "body", nullable = false, length = 500)
    val body: String,

    @Column(name = "business_date", nullable = false)
    val businessDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "fcm_result", nullable = false, length = 16)
    val fcmResult: FcmSendStatus,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant,
) {
    companion object {
        fun of(
            target: SendTarget,
            type: NotificationType,
            message: RenderedMessage,
            businessDate: LocalDate,
            fcmResult: FcmSendStatus,
        ): NotificationLog = NotificationLog(
            userId = target.userId,
            notificationType = type,
            messageTone = message.actualTone,
            variableApplied = message.variableApplied,
            title = message.title,
            body = message.body,
            businessDate = businessDate,
            fcmResult = fcmResult,
            sentAt = Instant.now(),
        )
    }
}
