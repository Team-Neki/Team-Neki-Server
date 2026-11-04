package com.neki.domain.notification.models

import java.time.LocalDate

/**
 * fileName       : Holiday
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 공휴일 한 날. HOLIDAY_EXPLORE 의 발송일 = date + notifyOffsetDays (전날 -1, 당일 0)
 */
data class Holiday(val date: LocalDate, val name: String, val notifyOffsetDays: Int) {
    val notifyDate: LocalDate
        get() = date.plusDays(notifyOffsetDays.toLong())
}
