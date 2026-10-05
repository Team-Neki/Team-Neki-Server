package com.neki.batch.notification.tasklet

import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationRepository

/** WEEKEND_EXPLORE: 동의자 전원, 변수 없음 (docs/lld/notification-push/weekend-explore-job.md) */
class WeekendExploreTargetReader(private val notificationRepository: NotificationRepository) : SendTargetReader {

    override fun readPage(afterUserId: Long, size: Int): SendTargetPage {
        val page: List<Notification> = notificationRepository.findPushAgreedAfter(afterUserId, size)
        return SendTargetPage(
            targets = page.map { SendTarget(userId = it.userId, deviceToken = it.deviceToken) },
            nextCursor = SendTargetPage.cursorOf(page, size),
        )
    }
}
