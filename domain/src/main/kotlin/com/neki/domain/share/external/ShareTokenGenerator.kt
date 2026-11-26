package com.neki.domain.share.external

/**
 * fileName       : ShareTokenGenerator
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 추측 불가능한 공유 토큰 생성
 */
interface ShareTokenGenerator {
    fun generate(): String
}
