package com.neki.auth.application.port

/**
 * fileName       : NicknameGeneratorPort
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : 닉네임 생성 Port 인터페이스
 */
interface NicknameGeneratorPort {
    fun generateUniqueNickname(): String
}
