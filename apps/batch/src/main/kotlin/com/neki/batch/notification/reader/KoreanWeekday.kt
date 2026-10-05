package com.neki.batch.notification.reader

import java.time.DayOfWeek
import java.time.LocalDate

/** [최근 업로드 요일] 표기. "지난 토요일". 카피 톤을 바꾸려면 여기 한 곳 */
internal object KoreanWeekday {

    private val LABEL: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "월요일",
        DayOfWeek.TUESDAY to "화요일",
        DayOfWeek.WEDNESDAY to "수요일",
        DayOfWeek.THURSDAY to "목요일",
        DayOfWeek.FRIDAY to "금요일",
        DayOfWeek.SATURDAY to "토요일",
        DayOfWeek.SUNDAY to "일요일",
    )

    fun recentUploadLabel(date: LocalDate): String = "지난 ${LABEL.getValue(date.dayOfWeek)}"
}
