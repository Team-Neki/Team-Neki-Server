package com.neki.api.media.application.usecase

import com.neki.api.media.application.StoreMediaDirectlyUseCase
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.media.repository.MediaRepository
import com.neki.domain.media.service.MediaDirectUploadService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class StoreMediaDirectlyUseCaseTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var mediaStorage: MediaStorage
    private lateinit var useCase: StoreMediaDirectlyUseCase

    @BeforeEach
    fun setUp() {
        mediaRepository = mockk()
        mediaStorage = mockk()
        useCase = StoreMediaDirectlyUseCase(
            MediaDirectUploadService(mediaRepository, mediaStorage),
            FakeTransactionRunner(),
        )
    }

    private fun png(width: Int, height: Int): ByteArray = ByteBuffer.allocate(33)
        .put(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        .putInt(13)
        .put("IHDR".toByteArray())
        .putInt(width)
        .putInt(height)
        .array()

    private fun jpeg(width: Int, height: Int): ByteArray = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08,
        (height shr 8).toByte(), height.toByte(), (width shr 8).toByte(), width.toByte(),
        0x03, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    )

    @Test
    @DisplayName("PNG - put 후 실측 크기의 UPLOADED media 를 저장한다")
    fun storesPng() {
        // Given
        val content: ByteArray = png(1024, 1536)
        val saved = slot<Media>()
        every { mediaStorage.put(any(), any(), any()) } just Runs
        every { mediaRepository.save(capture(saved)) } answers {
            val m = firstArg<Media>()
            Media(
                id = 7L,
                storageKey = m.storageKey,
                ownerId = m.ownerId,
                mediaType = m.mediaType,
                status = m.status,
                contentType = m.contentType,
                width = m.width,
                height = m.height,
                size = m.size,
            )
        }

        // When
        val result = useCase.execute(MediaCommand.StoreMedia(1L, MediaType.POSE, content))

        // Then
        result.mediaId shouldBe 7L
        result.storageKey shouldStartWith "pose/"
        result.storageKey shouldEndWith ".png"
        saved.captured.status shouldBe MediaStatus.UPLOADED
        saved.captured.contentType shouldBe "image/png"
        saved.captured.width shouldBe 1024
        saved.captured.height shouldBe 1536
        saved.captured.size shouldBe content.size.toLong()
        verifyOrder {
            mediaStorage.put(result.storageKey, "image/png", content)
            mediaRepository.save(any())
        }
    }

    @Test
    @DisplayName("JPEG - storageKey 확장자는 감지한 형식(jpg)이다")
    fun jpegExtensionFollowsDetectedType() {
        // Given
        every { mediaStorage.put(any(), any(), any()) } just Runs
        every { mediaRepository.save(any()) } answers {
            val m = firstArg<Media>()
            Media(
                id = 8L,
                storageKey = m.storageKey,
                ownerId = m.ownerId,
                mediaType = m.mediaType,
                contentType = m.contentType,
            )
        }

        // When
        val result = useCase.execute(MediaCommand.StoreMedia(1L, MediaType.POSE, jpeg(640, 480)))

        // Then
        result.storageKey shouldEndWith ".jpg"
        verify { mediaStorage.put(any(), "image/jpeg", any()) }
    }

    @Test
    @DisplayName("이미지가 아니면 UPLOAD_FAILED 이고 put 을 호출하지 않는다")
    fun rejectsNonImage() {
        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(MediaCommand.StoreMedia(1L, MediaType.POSE, "hello,world".toByteArray()))
        }
        ex.resultCode shouldBe ResultCode.UPLOAD_FAILED
        verify(exactly = 0) { mediaStorage.put(any(), any(), any()) }
        verify(exactly = 0) { mediaRepository.save(any()) }
    }

    @Test
    @DisplayName("DB 저장 실패 - 올린 객체를 지우고 예외를 다시 던진다")
    fun compensatesWhenSaveFails() {
        // Given
        val key = slot<String>()
        every { mediaStorage.put(capture(key), any(), any()) } just Runs
        every { mediaRepository.save(any()) } throws RuntimeException("db down")
        every { mediaStorage.deleteByKey(any()) } just Runs

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(MediaCommand.StoreMedia(1L, MediaType.POSE, png(10, 20)))
        }.message shouldBe "db down"
        verify(exactly = 1) { mediaStorage.deleteByKey(key.captured) }
    }

    @Test
    @DisplayName("DB 저장 실패 후 보상도 실패하면 원래 예외를 던진다")
    fun rethrowsOriginalWhenCompensationFails() {
        // Given
        every { mediaStorage.put(any(), any(), any()) } just Runs
        every { mediaRepository.save(any()) } throws RuntimeException("db down")
        every { mediaStorage.deleteByKey(any()) } throws RuntimeException("s3 down")

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(MediaCommand.StoreMedia(1L, MediaType.POSE, png(10, 20)))
        }.message shouldBe "db down"
    }
}
