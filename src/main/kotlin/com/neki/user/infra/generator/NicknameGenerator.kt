package com.neki.user.infra.generator

import com.neki.auth.application.port.NicknameGeneratorPort
import com.neki.user.infra.persist.jpa.UserRepository
import org.springframework.stereotype.Component

/**
 * fileName       : NicknameGenerator
 * author         : koo
 * date           : 2026. 2. 28.
 * description    : 랜덤 닉네임 생성기 구현체
 */
@Component
class NicknameGenerator(private val userRepository: UserRepository) : NicknameGeneratorPort {

    companion object {
        private val ADJECTIVES = listOf(
            "사진찍는", "셔터누른", "순간담은", "기록하는",
            "스냅찍는", "빛나는", "반짝이는", "선명한", "아련한",
            "감성적인", "필름같은", "빈티지한",
        )

        private val NOUNS = listOf(
            "라이언", "고양이", "토끼", "강아지", "곰돌이",
            "카메라", "렌즈", "필름", "셔터", "프레임",
            "스냅", "앨범", "사진사", "순간", "추억", "기억",
        )

        private const val MAX_RETRY_COUNT = 10
        private const val MIN_NUMBER = 1
        private const val MAX_NUMBER = 99
        private const val FALLBACK_MODULO = 100
        private const val MAX_LENGTH = 10
    }

    override fun generateUniqueNickname(): String {
        repeat(MAX_RETRY_COUNT) {
            val adjective: String = ADJECTIVES.random()
            val noun: String = NOUNS.random()
            val number: Int = (MIN_NUMBER..MAX_NUMBER).random()
            val nickname = "$adjective$noun-$number"
            if (nickname.length <= MAX_LENGTH && !userRepository.existsByName(nickname)) {
                return nickname
            }
        }

        // fallback: 타임스탬프 기반 2자리
        val adjective = ADJECTIVES.random()
        val noun = NOUNS.random()
        val nickname = "$adjective$noun-${System.currentTimeMillis() % FALLBACK_MODULO}"
        return nickname.take(MAX_LENGTH)
    }
}
