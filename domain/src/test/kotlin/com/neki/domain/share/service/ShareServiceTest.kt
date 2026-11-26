package com.neki.domain.share.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.share.dto.ShareCommand
import com.neki.domain.share.dto.ShareQuery
import com.neki.domain.share.external.ShareTokenGenerator
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.repository.ShareLinkRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : ShareServiceTest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : ShareService 발급·조회 판정(404/410) 단위 테스트
 */
class ShareServiceTest :
    FunSpec({

        val shareLinkRepository = mockk<ShareLinkRepository>()
        val tokenGenerator = mockk<ShareTokenGenerator>()
        val shareService = ShareService(shareLinkRepository, tokenGenerator)

        val now: LocalDateTime = LocalDateTime.of(2026, 11, 26, 12, 0, 0)
        val validToken: String = "A".repeat(43)

        beforeTest { clearMocks(shareLinkRepository, tokenGenerator) }

        fun link(expiresAt: LocalDateTime): ShareLink =
            ShareLink(token = validToken, userId = 1L, photoId = 2L, expiresAt = expiresAt)

        test("getViewableLink - 형식이 틀린 토큰은 DB 를 조회하지 않고 SHARE_NOT_FOUND") {
            // When
            val ex: BusinessException = shouldThrow<BusinessException> {
                shareService.getViewableLink(ShareQuery.ViewSharedImage("abc"), now)
            }

            // Then
            ex.resultCode shouldBe ResultCode.SHARE_NOT_FOUND
            verify(exactly = 0) { shareLinkRepository.getByToken(any()) }
        }

        test("getViewableLink - 없는 토큰은 SHARE_NOT_FOUND") {
            // Given
            every { shareLinkRepository.getByToken(validToken) } returns null

            // When
            val ex: BusinessException = shouldThrow<BusinessException> {
                shareService.getViewableLink(ShareQuery.ViewSharedImage(validToken), now)
            }

            // Then
            ex.resultCode shouldBe ResultCode.SHARE_NOT_FOUND
        }

        test("getViewableLink - 만료 시각과 같으면 SHARE_EXPIRED") {
            // Given
            every { shareLinkRepository.getByToken(validToken) } returns link(expiresAt = now)

            // When
            val ex: BusinessException = shouldThrow<BusinessException> {
                shareService.getViewableLink(ShareQuery.ViewSharedImage(validToken), now)
            }

            // Then
            ex.resultCode shouldBe ResultCode.SHARE_EXPIRED
        }

        test("getViewableLink - 만료 전이면 링크를 돌려준다") {
            // Given
            val alive: ShareLink = link(expiresAt = now.plusSeconds(1))
            every { shareLinkRepository.getByToken(validToken) } returns alive

            // When
            val result: ShareLink = shareService.getViewableLink(ShareQuery.ViewSharedImage(validToken), now)

            // Then
            result shouldBeSameInstanceAs alive
        }

        test("requireViewablePhoto - 사진이 없으면 SHARE_EXPIRED") {
            val ex: BusinessException = shouldThrow<BusinessException> { shareService.requireViewablePhoto(null) }

            ex.resultCode shouldBe ResultCode.SHARE_EXPIRED
        }

        test("requireOwnedPhoto - 사진이 없으면 NOT_FOUND") {
            val ex: BusinessException = shouldThrow<BusinessException> { shareService.requireOwnedPhoto(null) }

            ex.resultCode shouldBe ResultCode.NOT_FOUND
        }

        test("requireOwnedPhoto - 사진이 있으면 그대로 돌려준다") {
            val photo = SharePhoto(photoId = 2L, userId = 1L, storageKey = "photo-booth/a.jpg")

            shareService.requireOwnedPhoto(photo) shouldBe photo
        }

        test("createLink - 생성기 토큰과 (now + expiration) 을 초 단위로 자른 만료로 저장한다") {
            // Given
            val saved = slot<ShareLink>()
            every { tokenGenerator.generate() } returns validToken
            every { shareLinkRepository.save(capture(saved)) } answers { saved.captured }
            val nowWithNanos: LocalDateTime = now.withNano(123_456_789)

            // When
            val result: ShareLink = shareService.createLink(
                ShareCommand.IssueShareLink(userId = 1L, photoId = 2L),
                nowWithNanos,
                Duration.ofDays(7),
            )

            // Then
            result.token shouldBe validToken
            result.userId shouldBe 1L
            result.photoId shouldBe 2L
            result.expiresAt shouldBe now.plusDays(7)
        }

        test("findReusableLink - userId, photoId, now 로 살아있는 링크를 찾는다") {
            // Given
            val alive: ShareLink = link(expiresAt = now.plusDays(1))
            every { shareLinkRepository.getLatestAlive(1L, 2L, now) } returns alive

            // When
            val result: ShareLink? = shareService.findReusableLink(ShareCommand.IssueShareLink(1L, 2L), now)

            // Then
            result shouldBeSameInstanceAs alive
        }
    })
