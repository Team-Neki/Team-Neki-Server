package com.neki.api.search.infra.scheduler

import com.neki.domain.search.models.qu.EntityType
import com.neki.domain.search.service.qu.QueryUnderstandingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.util.concurrent.TimeUnit

/**
 * fileName       : SearchDictionaryRefresher
 * author         : koo
 * date           : 2026. 10. 4.
 * description    : NER 사전을 앱이 뜰 때 메모리에 올리고 10분마다 다시 만든다. batch 는 검색어를 해석하지 않으므로 apps/api 에만 둔다.
 *   새 사전을 다 만든 뒤 참조 하나만 바꿔 끼우므로(@Volatile) 요청은 갱신 중에도 이전 사전이나 새 사전 하나를 통째로 본다.
 */
@Component
class SearchDictionaryRefresher(private val queryUnderstandingService: QueryUnderstandingService) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 앱이 뜰 때는 readiness 가 열리기 전에 동기로 올리고, 이후 이전 갱신이 끝난 시점부터 10분마다 다시 만든다 (겹쳐 돌지 않는다).
     * 실패하면 이전 사전을 그대로 쓴다. 처음부터 실패하면 자동완성 keyword 는 그대로 동작하고,
     * 자유 검색어만 지역·역을 찾지 못해 빈 결과가 된다. 다음 주기에 다시 시도한다.
     */
    @EventListener(ApplicationReadyEvent::class)
    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun refresh() {
        runCatching { queryUnderstandingService.reloadDictionary() }
            .onSuccess { sizeByType ->
                // 갈아 끼운 직후 JVM 힙 전체 스냅샷. 이전 사전은 GC 전까지 used 에 남아 있어 사전 크기 자체는 아니다
                val heap: MemoryUsage = ManagementFactory.getMemoryMXBean().heapMemoryUsage
                log.info(
                    "[SEARCH] dictionary refreshed entries={} region={} station={} booth={} brand={} " +
                        "heapUsedMb={} heapCommittedMb={} heapMaxMb={}",
                    sizeByType.values.sum(),
                    sizeByType.getValue(EntityType.REGION),
                    sizeByType.getValue(EntityType.STATION),
                    sizeByType.getValue(EntityType.BRANCH),
                    sizeByType.getValue(EntityType.BRAND),
                    heap.used / BYTES_PER_MB,
                    heap.committed / BYTES_PER_MB,
                    heap.max / BYTES_PER_MB,
                )
            }
            .onFailure { log.error("[SEARCH] dictionary refresh failed, keeping previous dictionary", it) }
    }

    companion object {
        private const val BYTES_PER_MB: Long = 1024 * 1024
    }
}
