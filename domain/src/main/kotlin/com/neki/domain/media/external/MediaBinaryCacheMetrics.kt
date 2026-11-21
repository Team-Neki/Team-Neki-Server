package com.neki.domain.media.external

import com.neki.domain.media.models.MediaType

/**
 * 이미지 바이너리 캐시 조회 결과 기록 포트. 구현(Micrometer)은 infra 에 둔다.
 */
interface MediaBinaryCacheMetrics {

    /**
     * 요청당 첫 캐시 조회 결과를 한 번 기록한다
     */
    fun recordLookup(mediaType: MediaType, hit: Boolean)

    companion object {
        val NOOP: MediaBinaryCacheMetrics = object : MediaBinaryCacheMetrics {
            override fun recordLookup(mediaType: MediaType, hit: Boolean) = Unit
        }
    }
}
