package com.neki.batch.notification.holiday

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.math.abs

/**
 * fileName       : HolidayCalendar
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 클래스패스 holidays.csv 를 읽어 발송일을 판정한다. one-shot 이라 기동 시 한 번 읽으면 끝이다.
 *                  형식: holiday_date,name,notify_offset_days (ISO 날짜). 주석(#), 빈 줄, 헤더 허용. offset 생략 시 0.
 *                  파일이 없거나 형식이 틀리면 빈 생성에서 실패해 잡이 FAILED 로 끝난다. 조용히 0건이 되지 않는다
 */
@Component
class HolidayCalendar(@Value("\${neki.batch.holiday-csv}") resourcePath: String) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val holidays: List<Holiday> = load(resourcePath)

    /** businessDate 가 발송일인 공휴일. 없으면 null. 둘 이상이면 파일 순서상 첫 행 */
    fun holidayOn(businessDate: LocalDate): Holiday? = holidays.firstOrNull { it.notifyDate == businessDate }

    private fun load(path: String): List<Holiday> {
        val loaded: List<Holiday> = ClassPathResource(
            path,
        ).inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(HEADER_PREFIX) }
                .map(::parse)
                .toList()
        }
        loaded
            .filter { abs(it.notifyOffsetDays) > MAX_OFFSET_DAYS }
            .forEach {
                log.warn(
                    "공휴일 '{}' 의 notify_offset_days={} 가 +-{} 일을 넘습니다. CSV 를 점검하세요",
                    it.name,
                    it.notifyOffsetDays,
                    MAX_OFFSET_DAYS,
                )
            }
        return loaded
    }

    private fun parse(line: String): Holiday {
        val cols: List<String> = line.split(",").map { it.trim() }
        return Holiday(
            date = LocalDate.parse(cols[0]),
            name = cols[1],
            notifyOffsetDays = cols.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0,
        )
    }

    private companion object {
        const val HEADER_PREFIX = "holiday_date"

        /** 전날(-1)·당일(0) 에 여유를 둔 데이터 위생 경고 기준. 판정은 막지 않는다 */
        const val MAX_OFFSET_DAYS = 2
    }
}
