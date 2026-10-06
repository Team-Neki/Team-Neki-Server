package com.neki.domain.notification.infra.csv

import com.neki.domain.notification.models.Holiday
import com.neki.domain.notification.repository.HolidayRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Repository
import java.time.LocalDate
import kotlin.math.abs

/**
 * fileName       : CsvHolidayRepositoryAdapter
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 클래스패스 holidays.csv (domain 리소스) 를 빈 생성 시 한 번 읽는다. 갱신은 배포다.
 *                  형식: holiday_date,name,notify_offset_days (ISO 날짜). 주석(#), 빈 줄, 헤더 허용. offset 생략 시 0.
 *                  파일이 없거나 형식이 틀리면 기동이 실패한다. 조용히 0건이 되지 않는다
 */
@Repository
class CsvHolidayRepositoryAdapter(@Value("\${neki.notification.holiday-csv:holidays.csv}") resourcePath: String) :
    HolidayRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private val holidays: List<Holiday> = load(resourcePath)

    override fun findByNotifyDate(date: LocalDate): Holiday? = holidays.firstOrNull { it.notifyDate == date }

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
