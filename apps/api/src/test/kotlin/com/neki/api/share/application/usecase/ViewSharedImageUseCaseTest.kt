package com.neki.api.share.application.usecase

import com.neki.api.share.application.ViewSharedImageUseCase
import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.share.client.PhotoClient
import com.neki.domain.share.dto.ShareQuery
import com.neki.domain.share.external.SharedImageDelivery
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.models.SharedImageContent
import com.neki.domain.share.service.ShareService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ViewSharedImageUseCaseTest {

    private lateinit var shareService: ShareService
    private lateinit var photoClient: PhotoClient
    private lateinit var delivery: SharedImageDelivery
    private lateinit var useCase: ViewSharedImageUseCase

    private val cacheMaxAge: Duration = Duration.ofHours(1)
    private val query = ShareQuery.ViewSharedImage(token = "A".repeat(43))
    private val photo = SharePhoto(photoId = 2L, userId = 1L, storageKey = "photo-booth/a.jpg")
    private val binary = SharedImageContent.Binary(byteArrayOf(1, 2, 3), "image/jpeg")

    @BeforeEach
    fun setUp() {
        shareService = mockk()
        photoClient = mockk()
        delivery = mockk()
        useCase = ViewSharedImageUseCase(
            shareService,
            photoClient,
            delivery,
            ShareProperties(
                expiration = Duration.ofDays(7),
                cacheMaxAge = cacheMaxAge,
                webUrl = "https://share.example.com",
            ),
        )
    }

    private fun givenLink(expiresAt: LocalDateTime): ShareLink {
        val link = ShareLink(token = query.token, userId = 1L, photoId = 2L, expiresAt = expiresAt)
        every { shareService.getViewableLink(query, any()) } returns link
        every { photoClient.findSharablePhoto(1L, 2L) } returns photo
        every { shareService.requireViewablePhoto(photo) } returns photo
        return link
    }

    @Test
    @DisplayName("남은 시간이 cacheMaxAge 보다 길면 maxAge 는 cacheMaxAge")
    fun `남은 시간이 길면 cacheMaxAge`() {
        // Given
        val link: ShareLink = givenLink(LocalDateTime.now().plusDays(3))
        every { delivery.deliver(photo, cacheMaxAge) } returns binary

        // When
        val result: ShareResult.ViewSharedImage = useCase.execute(query)

        // Then
        result.maxAge shouldBe cacheMaxAge
        result.expiresAt shouldBe link.expiresAt
        result.content shouldBeSameInstanceAs binary
    }

    @Test
    @DisplayName("남은 시간이 cacheMaxAge 보다 짧으면 maxAge 는 남은 시간이고 전달에도 같은 값을 넘긴다")
    fun `남은 시간이 짧으면 남은 시간`() {
        // Given
        givenLink(LocalDateTime.now().plusMinutes(10))
        val validFor = slot<Duration>()
        every { delivery.deliver(photo, capture(validFor)) } returns binary

        // When
        val result: ShareResult.ViewSharedImage = useCase.execute(query)

        // Then
        result.maxAge shouldBeLessThanOrEqualTo Duration.ofMinutes(10)
        result.maxAge shouldBeGreaterThan Duration.ofMinutes(9)
        validFor.captured shouldBe result.maxAge
    }

    @Test
    @DisplayName("Redirect 전달 결과도 그대로 넘긴다")
    fun `Redirect 결과를 그대로 넘긴다`() {
        // Given
        givenLink(LocalDateTime.now().plusDays(1))
        val redirect = SharedImageContent.Redirect("https://cdn.example.com/signed")
        every { delivery.deliver(photo, any()) } returns redirect

        // When
        val result: ShareResult.ViewSharedImage = useCase.execute(query)

        // Then
        result.content shouldBe redirect
    }

    @Test
    @DisplayName("원본 사진을 볼 수 없으면 SHARE_EXPIRED 이고 전달하지 않는다")
    fun `원본 사진이 없으면 SHARE_EXPIRED`() {
        // Given
        val link =
            ShareLink(token = query.token, userId = 1L, photoId = 2L, expiresAt = LocalDateTime.now().plusDays(1))
        every { shareService.getViewableLink(query, any()) } returns link
        every { photoClient.findSharablePhoto(1L, 2L) } returns null
        every { shareService.requireViewablePhoto(null) } throws BusinessException(ResultCode.SHARE_EXPIRED)

        // When
        val ex: BusinessException = shouldThrow<BusinessException> { useCase.execute(query) }

        // Then
        ex.resultCode shouldBe ResultCode.SHARE_EXPIRED
        verify(exactly = 0) { delivery.deliver(any(), any()) }
    }
}
