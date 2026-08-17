package com.neki.domain.media.infra.lock.redis

import java.time.Duration

/**
 * 분산 락 타이밍 파라미터.
 *
 * lockTtl은 홀더가 죽었을 때 락을 자동 해제하기 위한 안전장치로, 원본 스토리지 조회 소요 시간을 기준으로 잡는다.
 * 대기자는 maxRetries/initialDelayMs/multiplier로 계산되는 재시도 예산을 모두 쓰면 lockTtl과 무관하게 포기한다.
 */
data class DistributedLockProperties(
    val lockTtl: Duration = Duration.ofSeconds(10),
    val maxRetries: Int = 5,
    val initialDelayMs: Long = 50,
    val maxDelayMs: Long = 500,
    val multiplier: Double = 1.5,
) {
    companion object {
        val DEFAULT = DistributedLockProperties()
    }
}
