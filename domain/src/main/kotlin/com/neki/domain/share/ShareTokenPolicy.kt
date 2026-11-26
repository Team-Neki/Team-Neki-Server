package com.neki.domain.share

/**
 * fileName       : ShareTokenPolicy
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 토큰 형식 규칙. SecureRandom 32바이트(256bit)를 Base64 URL-safe 무패딩으로 인코딩한 43자
 */
object ShareTokenPolicy {
    const val TOKEN_BYTES: Int = 32
    const val TOKEN_LENGTH: Int = 43

    private val FORMAT = Regex("^[A-Za-z0-9_-]{$TOKEN_LENGTH}$")

    /** 형식이 틀린 토큰은 DB 를 조회하지 않고 바로 거른다 */
    fun isWellFormed(token: String): Boolean = FORMAT.matches(token)
}
