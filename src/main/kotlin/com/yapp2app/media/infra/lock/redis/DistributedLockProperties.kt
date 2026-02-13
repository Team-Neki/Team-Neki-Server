package com.yapp2app.media.infra.lock.redis

data class DistributedLockProperties(
    val maxRetries: Int = 5,
    val initialDelayMs: Long = 50,
    val maxDelayMs: Long = 500,
    val multiplier: Double = 1.5,
) {
    companion object {
        val DEFAULT = DistributedLockProperties()
    }
}
