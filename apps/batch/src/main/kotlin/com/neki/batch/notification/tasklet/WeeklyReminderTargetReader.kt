package com.neki.batch.notification.step

import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.photo.repository.PhotoImageRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * WEEKLY_REMINDER: D-7 당일에 (삭제되지 않은) 사진을 올린 동의자. 변수는 마지막 업로드의 요일
 * (docs/lld/notification-push/weekly-reminder-job.md). 삭제 제외는 PhotoImage 의 @SQLRestriction 이 한다
 */
class WeeklyReminderTargetReader(
    businessDate: LocalDate,
    private val notificationRepository: NotificationRepository,
    private val photoImageRepository: PhotoImageRepository,
) : SendTargetReader {

    private val uploadDayStart: LocalDateTime = businessDate.minusDays(7).atStartOfDay()
    private val uploadDayEnd: LocalDateTime = uploadDayStart.plusDays(1)

    override fun readPage(afterUserId: Long, size: Int): SendTargetPage {
        val page: List<Notification> = notificationRepository.findPushAgreedAfter(afterUserId, size)
        val uploaded: Set<Long> =
            photoImageRepository.findUserIdsUploadedBetween(page.map { it.userId }, uploadDayStart, uploadDayEnd)
        val kept: List<Notification> = page.filter { it.userId in uploaded }
        val lastUploadedAt: Map<Long, LocalDateTime> =
            photoImageRepository.findLastUploadedAtByUserIds(kept.map { it.userId })

        return SendTargetPage(
            targets = kept.map {
                SendTarget(
                    userId = it.userId,
                    deviceToken = it.deviceToken,
                    variables = mapOf(
                        MessageVariable.RECENT_UPLOAD_DAY to
                            lastUploadedAt[it.userId]?.let(::recentUploadLabel),
                    ),
                )
            },
            nextCursor = SendTargetPage.cursorOf(page, size),
        )
    }

    /** [최근 업로드 요일] 표기. "지난 토요일". 카피 톤을 바꾸려면 여기 한 곳 */
    private fun recentUploadLabel(at: LocalDateTime): String = "지난 ${WEEKDAY.getValue(at.dayOfWeek)}"

    companion object {
        private val WEEKDAY: Map<DayOfWeek, String> = mapOf(
            DayOfWeek.MONDAY to "월요일",
            DayOfWeek.TUESDAY to "화요일",
            DayOfWeek.WEDNESDAY to "수요일",
            DayOfWeek.THURSDAY to "목요일",
            DayOfWeek.FRIDAY to "금요일",
            DayOfWeek.SATURDAY to "토요일",
            DayOfWeek.SUNDAY to "일요일",
        )
    }
}
