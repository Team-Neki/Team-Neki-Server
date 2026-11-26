package com.neki.domain.share.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : ShareLinkTest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : ShareLink 만료 판정, 남은 시간, 발급 시 초 단위 절사 단위 테스트
 */
class ShareLinkTest :
    FunSpec({

        val expiresAt: LocalDateTime = LocalDateTime.of(2026, 12, 3, 12, 0, 0)

        fun link(): ShareLink = ShareLink(token = "t", userId = 1L, photoId = 2L, expiresAt = expiresAt)

        test("isExpired - 만료 시각 이전이면 만료가 아니다") {
            link().isExpired(expiresAt.minusNanos(1)) shouldBe false
        }

        test("isExpired - 만료 시각과 같으면 만료다") {
            link().isExpired(expiresAt) shouldBe true
        }

        test("isExpired - 만료 시각 이후면 만료다") {
            link().isExpired(expiresAt.plusSeconds(1)) shouldBe true
        }

        test("remaining - 만료 전에는 만료까지 남은 시간") {
            val result: Duration = link().remaining(expiresAt.minusMinutes(10))

            result shouldBe Duration.ofMinutes(10)
        }

        test("remaining - 만료 후에는 ZERO") {
            val result: Duration = link().remaining(expiresAt.plusDays(1))

            result shouldBe Duration.ZERO
        }

        test("issue - now + validFor 를 초 단위로 자른 값을 expiresAt 으로 쓴다") {
            // Given
            val now: LocalDateTime = LocalDateTime.of(2026, 11, 26, 12, 0, 0, 987_654_321)

            // When
            val issued: ShareLink = ShareLink.issue("token", 1L, 2L, now, Duration.ofDays(7))

            // Then
            issued.expiresAt shouldBe LocalDateTime.of(2026, 12, 3, 12, 0, 0)
            issued.expiresAt.nano shouldBe 0
            issued.token shouldBe "token"
            issued.userId shouldBe 1L
            issued.photoId shouldBe 2L
            issued.deletedAt shouldBe null
        }
    })
