package com.neki.media.application.usecase

import com.neki.media.application.command.ConfirmMediasUploadedCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.ConfirmMediasUploadedResult.UploadConfirmStatus
import com.neki.media.domain.entity.MediaStatus
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aMedia
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : ConfirmMediaUploadedUseCaseTest
 * description    : ConfirmMediaUploadedUseCase 단위 테스트
 */
class ConfirmMediaUploadedUseCaseTest : FunSpec({

    lateinit var mediaRepository: MediaRepositoryPort
    lateinit var mediaStorage: MediaStoragePort
    lateinit var useCase: ConfirmMediaUploadedUseCase

    beforeTest {
        mediaRepository = mockk()
        mediaStorage = mockk()
        useCase = ConfirmMediaUploadedUseCase(
            mediaRepository = mediaRepository,
            mediaStorage = mediaStorage,
            transactionRunner = FakeTransactionRunner(),
        )
    }

    test("빈 mediaIds - 빈 결과 반환") {
        // Given
        val command = ConfirmMediasUploadedCommand(ownerId = 1L, mediaIds = emptyList())

        // When
        val result = useCase.execute(command)

        // Then
        result.results shouldBe emptyMap()
        verify(exactly = 0) { mediaRepository.getMediaForUploadConfirmation(any(), any()) }
    }

    test("이미 업로드됨 - UPLOADED 상태이면 CONFIRMED 반환 (S3 체크 skip)") {
        // Given
        val ownerId = 1L
        val mediaId = 1L
        val uploadedMedia = aMedia(id = mediaId, ownerId = ownerId, status = MediaStatus.UPLOADED)

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, listOf(mediaId)) } returns listOf(uploadedMedia)

        // When
        val command = ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.results[mediaId] shouldBe UploadConfirmStatus.CONFIRMED
        verify(exactly = 0) { mediaStorage.exists(any()) }
    }

    test("미업로드 + S3 존재 - markAsUploaded 후 CONFIRMED 반환") {
        // Given
        val ownerId = 1L
        val mediaId = 2L
        val initiatedMedia = aMedia(id = mediaId, ownerId = ownerId, status = MediaStatus.INITIATED, storageKey = "pose/test.jpg")

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, listOf(mediaId)) } returns listOf(initiatedMedia)
        every { mediaStorage.exists("pose/test.jpg") } returns true

        // When
        val command = ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.results[mediaId] shouldBe UploadConfirmStatus.CONFIRMED
        initiatedMedia.status shouldBe MediaStatus.UPLOADED
    }

    test("미업로드 + S3 미존재 - NOT_UPLOADED 반환") {
        // Given
        val ownerId = 1L
        val mediaId = 3L
        val initiatedMedia = aMedia(id = mediaId, ownerId = ownerId, status = MediaStatus.INITIATED, storageKey = "pose/not-exist.jpg")

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, listOf(mediaId)) } returns listOf(initiatedMedia)
        every { mediaStorage.exists("pose/not-exist.jpg") } returns false

        // When
        val command = ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.results[mediaId] shouldBe UploadConfirmStatus.NOT_UPLOADED
    }

    test("미디어 미존재 - NOT_FOUND 반환") {
        // Given
        val ownerId = 1L
        val mediaId = 999L

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, listOf(mediaId)) } returns emptyList()

        // When
        val command = ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.results[mediaId] shouldBe UploadConfirmStatus.NOT_FOUND
    }

    test("S3 exists 체크 예외 - mediaStorage.exists 예외 발생 시 전파") {
        // Given
        val ownerId = 1L
        val mediaId = 4L
        val initiatedMedia = aMedia(id = mediaId, ownerId = ownerId, status = MediaStatus.INITIATED, storageKey = "pose/error.jpg")

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, listOf(mediaId)) } returns listOf(initiatedMedia)
        every { mediaStorage.exists("pose/error.jpg") } throws RuntimeException("S3 연결 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = listOf(mediaId)))
        }
    }

    test("rollback 멱등성 - 동일 mediaIds로 rollback 2회 호출 시 정상 동작") {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L)
        val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, status = MediaStatus.UPLOADED) }

        every { mediaRepository.getMediaForUploadConfirmation(ownerId, mediaIds) } returns medias

        val command = ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = mediaIds)

        // When
        useCase.rollback(command)
        useCase.rollback(command)

        // Then
        medias.forEach { it.status shouldBe MediaStatus.INITIATED }
        verify(exactly = 2) { mediaRepository.getMediaForUploadConfirmation(ownerId, mediaIds) }
    }
})
