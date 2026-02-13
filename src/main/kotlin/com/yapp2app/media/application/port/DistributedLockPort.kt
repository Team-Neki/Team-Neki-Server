package com.yapp2app.media.application.port

import java.time.Duration

/**
 * 분산 락 포트
 *
 * 여러 인스턴스 간 동시성 제어를 위한 분산 락 인터페이스입니다.
 */
interface DistributedLockPort {

    fun <T> executeWithLock(key: String, ttl: Duration, action: () -> T): T?
}
