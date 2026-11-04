package com.neki.batch.notification.tasklet

import com.neki.domain.notification.models.Holiday
import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.photo.repository.PhotoImageRepository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HOLIDAY_EXPLORE: businessDate 가 발송일인 공휴일이 있을 때만, D-1개월 이후 (삭제되지 않은) 업로드가 있는 동의자.
 * [공휴일명] 은 실행 단위 값이라 전원 같다 (docs/lld/notification-push/holiday-explore-job.md)
 */
class HolidayExploreTargetReader(
    businessDate: LocalDate,
    private val holiday: Holiday?,
    private val notificationRepository: NotificationRepository,
    private val photoImageRepository: PhotoImageRepository,
) : SendTargetReader {

    private val since: LocalDateTime = businessDate.minusMonths(1).atStartOfDay()

    override fun readPage(afterUserId: Long, size: Int): SendTargetPage {
        if (holiday == null) return SendTargetPage.EXHAUSTED

        val page: List<Notification> = notificationRepository.findPushAgreedAfter(afterUserId, size)
        val lastUploadedAt: Map<Long, LocalDateTime> =
            photoImageRepository.findLastUploadedAtByUserIds(page.map { it.userId })
        val variables: Map<MessageVariable, String?> = mapOf(MessageVariable.HOLIDAY_NAME to holiday.name)

        return SendTargetPage(
            targets = page
                .filter { (lastUploadedAt[it.userId] ?: return@filter false) >= since }
                .map { SendTarget(userId = it.userId, deviceToken = it.deviceToken, variables = variables) },
            nextCursor = SendTargetPage.cursorOf(page, size),
        )
    }
}
