package com.neki.domain.media.infra.metrics

import com.neki.domain.media.external.MediaBinaryCacheMetrics
import com.neki.domain.media.models.MediaType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * media.binary.cache{type, result=hit|miss} Counter. Prometheus 에서는 media_binary_cache_total.
 *
 * hit율은 요청당 첫 조회 기준이다. 락 대기 뒤 재확인에서 찾은 경우도 miss 로 센다.
 * 실제 S3 호출 수는 media.storage.fetch Timer 로 따로 본다.
 */
@Component
class MicrometerMediaBinaryCacheMetrics(private val meterRegistry: MeterRegistry) : MediaBinaryCacheMetrics {

    override fun recordLookup(mediaType: MediaType, hit: Boolean) {
        Counter.builder(CACHE_METRIC)
            .description("이미지 바이너리 캐시 첫 조회 결과 (요청당 1회)")
            .tag("type", mediaType.prefix)
            .tag("result", if (hit) RESULT_HIT else RESULT_MISS)
            .register(meterRegistry)
            .increment()
    }

    companion object {
        const val CACHE_METRIC = "media.binary.cache"
        const val RESULT_HIT = "hit"
        const val RESULT_MISS = "miss"
    }
}
