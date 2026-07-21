package com.neki.media.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.MediaType
import com.neki.media.application.command.GenerateUploadTicketCommand
import com.neki.media.application.contract.UploadTicket
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aMedia
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * fileName       : GenerateUploadTicketUseCaseTest
 * description    : GenerateUploadTicketUseCase 단위 테스트
 */
class GenerateUploadTicketUseCaseTest {

    private lateinit var mediaStorage: MediaStoragePort
    private lateinit var mediaRepository: MediaRepositoryPort
    private lateinit var useCase: GenerateUploadTicketUseCase

    private val fixedExpiresAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        mediaStorage = mockk()
        mediaRepository = mockk()
        useCase = GenerateUploadTicketUseCase(
            mediaStorage = mediaStorage,
            mediaRepository = mediaRepository,
            transactionRunner = FakeTransactionRunner(),
        )
    }

    @Test
    @DisplayName("정상 생성 - N개 아이템에 대해 N개 presigned URL 생성 + 미디어 저장")
    fun `정상 생성 - N개 아이템에 대해 N개 presigned URL 생성 + 미디어 저장`() {
        // Given
        val ownerId = 1L
        val items = listOf(
            GenerateUploadTicketCommand.UploadTicketItem(
                filename = "photo1.jpg",
                contentType = "image/jpeg",
                mediaType = MediaType.POSE,
            ),
            GenerateUploadTicketCommand.UploadTicketItem(
                filename = "photo2.jpg",
                contentType = "image/jpeg",
                mediaType = MediaType.POSE,
            ),
        )

        val savedMedia1 = aMedia(id = 1L, ownerId = ownerId)
        val savedMedia2 = aMedia(id = 2L, ownerId = ownerId)

        val ticket1 = UploadTicket(
            url = "https://s3.example.com/presigned-1",
            method = "PUT",
            expiresAt = fixedExpiresAt,
            contentType = "image/jpeg",
        )
        val ticket2 = UploadTicket(
            url = "https://s3.example.com/presigned-2",
            method = "PUT",
            expiresAt = fixedExpiresAt,
            contentType = "image/jpeg",
        )

        every {
            mediaRepository.save(match { it.ownerId == ownerId && it.mediaType == MediaType.POSE })
        } returnsMany listOf(savedMedia1, savedMedia2)
        every { mediaStorage.generateUploadTicket(any(), "image/jpeg") } returnsMany listOf(ticket1, ticket2)

        // When
        val command = GenerateUploadTicketCommand(ownerId = ownerId, items = items)
        val result = useCase.execute(command)

        // Then
        result.tickets.size shouldBe 2
        result.tickets[0].mediaId shouldBe 1L
        result.tickets[0].uploadUrl shouldBe "https://s3.example.com/presigned-1"
        result.tickets[1].mediaId shouldBe 2L
        result.tickets[1].uploadUrl shouldBe "https://s3.example.com/presigned-2"
        verify(exactly = 2) { mediaRepository.save(any()) }
        verify(exactly = 2) { mediaStorage.generateUploadTicket(any(), any()) }
    }

    @Test
    @DisplayName("method/expiresAt 추출 - 첫 번째 ticket에서 method와 expiresAt 추출 확인")
    fun `method_expiresAt 추출 - 첫 번째 ticket에서 method와 expiresAt 추출 확인`() {
        // Given
        val ownerId = 1L
        val items = listOf(
            GenerateUploadTicketCommand.UploadTicketItem(
                filename = "photo1.jpg",
                contentType = "image/jpeg",
                mediaType = MediaType.POSE,
            ),
            GenerateUploadTicketCommand.UploadTicketItem(
                filename = "photo2.png",
                contentType = "image/png",
                mediaType = MediaType.POSE,
            ),
        )

        val savedMedia1 = aMedia(id = 1L, ownerId = ownerId)
        val savedMedia2 = aMedia(id = 2L, ownerId = ownerId)

        val firstTicketExpiresAt = Instant.parse("2026-01-01T12:00:00Z")
        val secondTicketExpiresAt = Instant.parse("2026-01-02T12:00:00Z")

        val ticket1 = UploadTicket(
            url = "https://s3.example.com/presigned-1",
            method = "PUT",
            expiresAt = firstTicketExpiresAt,
            contentType = "image/jpeg",
        )
        val ticket2 = UploadTicket(
            url = "https://s3.example.com/presigned-2",
            method = "POST",
            expiresAt = secondTicketExpiresAt,
            contentType = "image/png",
        )

        every { mediaRepository.save(any()) } returnsMany listOf(savedMedia1, savedMedia2)
        every { mediaStorage.generateUploadTicket(any(), "image/jpeg") } returns ticket1
        every { mediaStorage.generateUploadTicket(any(), "image/png") } returns ticket2

        // When
        val command = GenerateUploadTicketCommand(ownerId = ownerId, items = items)
        val result = useCase.execute(command)

        // Then: 첫 번째 티켓 기준으로 method, expiresAt 추출
        result.method shouldBe "PUT"
        result.expiresAt shouldBe firstTicketExpiresAt
        result.expiresAt shouldNotBe secondTicketExpiresAt
    }

    @Test
    @DisplayName("빈 items 목록은 BusinessException 발생")
    fun `빈 items 목록은 BusinessException 발생`() {
        // Given
        val ownerId = 1L
        val command = GenerateUploadTicketCommand(ownerId = ownerId, items = emptyList())

        // When & Then
        shouldThrow<BusinessException> {
            useCase.execute(command)
        }.resultCode shouldBe ResultCode.INVALID_PARAMETER
    }
}
