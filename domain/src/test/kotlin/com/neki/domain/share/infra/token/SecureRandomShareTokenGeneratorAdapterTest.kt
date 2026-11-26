package com.neki.domain.share.infra.token

import com.neki.domain.share.ShareTokenPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * fileName       : SecureRandomShareTokenGeneratorAdapterTest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 토큰 길이·형식·중복 없음 단위 테스트
 */
class SecureRandomShareTokenGeneratorAdapterTest :
    FunSpec({

        val generator = SecureRandomShareTokenGeneratorAdapter()

        test("generate - 43자 URL-safe 토큰이고 ShareTokenPolicy 형식을 통과한다") {
            // When
            val token: String = generator.generate()

            // Then
            token.length shouldBe ShareTokenPolicy.TOKEN_LENGTH
            ShareTokenPolicy.isWellFormed(token) shouldBe true
        }

        test("generate - 1만 회 생성해도 중복이 없다") {
            // When
            val tokens: Set<String> = (1..10_000).map { generator.generate() }.toSet()

            // Then
            tokens shouldHaveSize 10_000
            tokens.all { ShareTokenPolicy.isWellFormed(it) } shouldBe true
        }
    })
