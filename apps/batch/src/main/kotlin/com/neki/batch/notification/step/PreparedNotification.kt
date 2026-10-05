package com.neki.batch.notification.step

import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.RenderedMessage
import com.neki.domain.notification.models.SendTarget
import java.time.LocalDate

/** Processor -> Writer 로 넘어가는 발송 확정 1건 */
data class PreparedNotification(
    val target: SendTarget,
    val type: NotificationType,
    val message: RenderedMessage,
    val businessDate: LocalDate,
)
