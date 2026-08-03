package com.neki.media

interface DistributedLock {

    /**
     * key 단위로 action을 한 번만 실행한다.
     * 락을 획득하지 못하면 null을 반환하므로 호출자가 폴백을 결정한다.
     * 락 TTL, 재시도 정책 등 타이밍 파라미터는 어댑터가 소유한다.
     */
    fun <T> executeWithLock(key: String, action: () -> T): T?
}
