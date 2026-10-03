package com.neki.api.search.infra.scheduler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.config.FixedDelayTask
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

/**
 * fileName       : SearchDictionaryRefresherTest
 * author         : koo
 * date           : 2026. 10. 4.
 * description    : NER 사전 갱신이 스케줄러에 10분 고정 지연으로 등록되는지 확인한다. @EnableScheduling 이 빠지면 실패한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SearchDictionaryRefresherTest {

    @Autowired
    private lateinit var scheduledTaskHolder: ScheduledTaskHolder

    @Test
    @DisplayName("NER 사전 갱신은 10분 뒤 시작해 이전 갱신이 끝난 시점부터 10분마다 돈다")
    fun refreshIsScheduledEveryTenMinutes() {
        val task: FixedDelayTask = scheduledTaskHolder.scheduledTasks
            .map { it.task }
            .filterIsInstance<FixedDelayTask>()
            .single { it.runnable.toString().endsWith("SearchDictionaryRefresher.refresh") }

        assertEquals(Duration.ofMinutes(10), task.intervalDuration)
        assertEquals(Duration.ofMinutes(10), task.initialDelayDuration)
    }
}
