package com.neki.domain.media.infra.cache

/**
 * Redis 캐시 키 네이밍 컨벤션 관리
 *
 * Media 도메인의 Redis 키 포맷을 중앙 관리합니다.
 * 포맷: media:binary:{objectKey}
 */
internal object MediaRedisCacheKey {
    private const val BINARY_PREFIX = "media:binary:"

    fun binaryKey(objectKey: String): String = "$BINARY_PREFIX$objectKey"
}
