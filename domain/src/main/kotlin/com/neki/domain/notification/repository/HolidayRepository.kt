package com.neki.domain.notification.repository

import com.neki.domain.notification.models.Holiday
import java.time.LocalDate

/**
 * fileName       : HolidayRepository
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 공휴일 포트. 원천(지금은 클래스패스 CSV, 추후 Google Sheet)은 infra 어댑터가 정한다
 */
interface HolidayRepository {

    /** notifyDate(공휴일 + offset) 가 date 인 공휴일. 없으면 null. 둘 이상이면 원천 순서상 첫 행 */
    fun findByNotifyDate(date: LocalDate): Holiday?
}
