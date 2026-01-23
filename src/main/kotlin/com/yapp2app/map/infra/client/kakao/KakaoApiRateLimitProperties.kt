package com.yapp2app.map.infra.client.kakao

/**
 * fileName       : KakaoApiRateLimitProperties
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : 카카오 API Rate Limiting 설정
 */
data class KakaoApiRateLimitProperties(
    val initialDelay: Long = 1000L, // 첫 API 호출 전 딜레이 (ms)
    val pageDelayMin: Long = 1000L, // 페이지 간 최소 딜레이 (ms)
    val pageDelayMax: Long = 2000L, // 페이지 간 최대 딜레이 (ms)
    val gridDelayMin: Long = 1000L, // 그리드 간 최소 딜레이 (ms)
    val gridDelayMax: Long = 2000L, // 그리드 간 최대 딜레이 (ms)
    val retryBackoffBase: Long = 10000L, // Retry 시 기본 딜레이 (10초)
) {
    companion object {
        /**
         * 카카오 API 기본 Rate Limit 설정
         */
        val DEFAULT = KakaoApiRateLimitProperties()
    }
}
