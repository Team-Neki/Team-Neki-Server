package com.neki.domain.user.external

/**
 * fileName       : NicknameGenerator
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : 닉네임 생성 Port 인터페이스
 */
interface NicknameGenerator {
    fun generateUniqueNickname(): String
}
