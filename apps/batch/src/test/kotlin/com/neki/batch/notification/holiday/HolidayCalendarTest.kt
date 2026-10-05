package com.neki.batch.notification.holiday

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** holidays-test.csv 에는 주석, 헤더, 빈 줄, offset 생략 행이 섞여 있어 파싱도 같이 검증된다 */
class HolidayCalendarTest {

    private val calendar = HolidayCalendar("holidays-test.csv")

    @Test
    fun `발송일이면 그 공휴일을 돌려준다`() {
        calendar.holidayOn(LocalDate.parse("2026-06-18"))?.name shouldBe "테스트공휴일"
    }

    @Test
    fun `notify_offset_days 가 발송일에 반영된다`() {
        calendar.holidayOn(LocalDate.parse("2026-06-19"))?.name shouldBe "전날알림"
        calendar.holidayOn(LocalDate.parse("2026-06-20")).shouldBeNull()
    }

    @Test
    fun `offset 을 생략하면 당일이다`() {
        calendar.holidayOn(LocalDate.parse("2026-12-25"))?.name shouldBe "성탄절"
    }

    @Test
    fun `발송일이 아니면 null`() {
        calendar.holidayOn(LocalDate.parse("2026-06-17")).shouldBeNull()
    }
}
