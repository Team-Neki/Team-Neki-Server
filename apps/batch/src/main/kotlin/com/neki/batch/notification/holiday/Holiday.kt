package com.neki.batch.notification.holiday

import java.time.LocalDate

/** 공휴일 한 날. 발송일 = date + notifyOffsetDays (전날 -1, 당일 0) */
data class Holiday(val date: LocalDate, val name: String, val notifyOffsetDays: Int) {
    val notifyDate: LocalDate
        get() = date.plusDays(notifyOffsetDays.toLong())
}
