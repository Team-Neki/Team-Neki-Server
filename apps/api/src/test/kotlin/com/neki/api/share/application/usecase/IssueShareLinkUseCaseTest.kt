package com.neki.api.share.application.usecase

import com.neki.api.share.application.IssueShareLinkUseCase
import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.share.client.PhotoClient
import com.neki.domain.share.dto.ShareCommand
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.service.ShareService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class IssueShareLinkUseCaseTest {

    private lateinit var shareService: ShareService
    private lateinit var photoClient: PhotoClient
    private lateinit var useCase: IssueShareLinkUseCase

    private val shareProperties =
        ShareProperties(
            expiration = Duration.ofDays(3),
            cacheMaxAge = Duration.ofHours(1),
            webUrl = "https://share.example.com",
        )
    private val command = ShareCommand.IssueShareLink(userId = 1L, photoId = 2L)
    private val photo = SharePhoto(photoId = 2L, userId = 1L, storageKey = "photo-booth/a.jpg")

    @BeforeEach
    fun setUp() {
        shareService = mockk()
        photoClient = mockk()
        useCase = IssueShareLinkUseCase(shareService, photoClient, shareProperties)
    }

    @Test
    @DisplayName("살아있는 링크가 있으면 재사용하고 새로 만들지 않는다")
    fun `살아있는 링크가 있으면 재사용한다`() {
        // Given
        val existing =
            ShareLink(token = "reused", userId = 1L, photoId = 2L, expiresAt = LocalDateTime.of(2026, 12, 1, 0, 0))
        every { photoClient.findSharablePhoto(1L, 2L) } returns photo
        every { shareService.requireOwnedPhoto(photo) } returns photo
        every { shareService.findReusableLink(command, any()) } returns existing

        // When
        val result: ShareResult.IssueShareLink = useCase.execute(command)

        // Then
        result shouldBe ShareResult.IssueShareLink(token = "reused", expiresAt = existing.expiresAt)
        verify(exactly = 0) { shareService.createLink(any(), any(), any()) }
    }

    @Test
    @DisplayName("살아있는 링크가 없으면 설정된 만료 기간으로 새로 만든다")
    fun `살아있는 링크가 없으면 새로 만든다`() {
        // Given
        val created =
            ShareLink(token = "new", userId = 1L, photoId = 2L, expiresAt = LocalDateTime.of(2026, 12, 1, 0, 0))
        every { photoClient.findSharablePhoto(1L, 2L) } returns photo
        every { shareService.requireOwnedPhoto(photo) } returns photo
        every { shareService.findReusableLink(command, any()) } returns null
        every { shareService.createLink(command, any(), Duration.ofDays(3)) } returns created

        // When
        val result: ShareResult.IssueShareLink = useCase.execute(command)

        // Then
        result.token shouldBe "new"
        verify(exactly = 1) { shareService.createLink(command, any(), Duration.ofDays(3)) }
    }

    @Test
    @DisplayName("소유한 사진이 아니면 NOT_FOUND 이고 링크를 만들지 않는다")
    fun `소유한 사진이 아니면 NOT_FOUND`() {
        // Given
        every { photoClient.findSharablePhoto(1L, 2L) } returns null
        every { shareService.requireOwnedPhoto(null) } throws BusinessException(ResultCode.NOT_FOUND)

        // When
        val ex: BusinessException = shouldThrow<BusinessException> { useCase.execute(command) }

        // Then
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { shareService.findReusableLink(any(), any()) }
        verify(exactly = 0) { shareService.createLink(any(), any(), any()) }
    }
}
