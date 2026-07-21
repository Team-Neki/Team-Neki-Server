package com.neki.pose.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.HeadCount
import com.neki.pose.application.command.UploadPosesCommand
import com.neki.pose.application.contract.MediaAvailability
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UploadPosesUseCaseTest {

    private lateinit var mediaClient: MediaClientPort
    private lateinit var poseRepository: PoseRepositoryPort
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var useCase: UploadPosesUseCase

    @BeforeEach
    fun setUp() {
        mediaClient = mockk()
        poseRepository = mockk()
        transactionRunner = FakeTransactionRunner()
        useCase = UploadPosesUseCase(mediaClient, transactionRunner, poseRepository)
    }

    private fun makeUploadItem(mediaId: Long, headCount: HeadCount = HeadCount.TWO): UploadPosesCommand.UploadItem =
        UploadPosesCommand.UploadItem(mediaId = mediaId, headCount = headCount, memo = null)

    private fun makeCommand(userId: Long = 1L, uploads: List<UploadPosesCommand.UploadItem>): UploadPosesCommand =
        UploadPosesCommand(userId = userId, uploads = uploads)

    @Test
    @DisplayName("정상 업로드 - 미디어 검증 → pose 저장")
    fun `정상 업로드 - 미디어 검증 → pose 저장`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE, 102L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } answers { firstArg() }

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { poseRepository.saveAll(any()) }
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("중복 mediaId → BusinessException(INVALID_PARAMETER)")
    fun `중복 mediaId → BusinessException(INVALID_PARAMETER)`() {
        // Given: 같은 mediaId가 두 번
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(101L))
        val command = makeCommand(uploads = uploads)

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("일부 미디어 UNAVAILABLE - 사용 가능한 미디어 롤백 후 BusinessException(UPLOAD_FAILED)")
    fun `일부 미디어 UNAVAILABLE - 사용 가능한 미디어 롤백 후 BusinessException(UPLOAD_FAILED)`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L), makeUploadItem(103L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L, 103L))
        } returns mapOf(
            101L to MediaAvailability.AVAILABLE,
            102L to MediaAvailability.UNAVAILABLE,
            103L to MediaAvailability.AVAILABLE,
        )
        every { mediaClient.rollbackMediasUploaded(1L, any()) } just Runs

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.UPLOAD_FAILED
        // 성공한 미디어 101, 103 롤백 확인
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(eq(1L), any()) }
        verify(exactly = 0) { poseRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("트랜잭션 실패 → 롤백 - 미디어 rollback 호출 확인")
    fun `트랜잭션 실패 → 롤백 - 미디어 rollback 호출 확인`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE, 102L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } throws RuntimeException("DB 저장 실패")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L, 102L)) } just Runs

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(101L, 102L)) }
    }

    @Test
    @DisplayName("롤백 중 예외 발생 - 롤백 예외가 전파됨")
    fun `롤백 중 예외 발생 - 롤백 예외가 전파됨`() {
        // Given
        val uploads = listOf(makeUploadItem(101L))
        val command = makeCommand(uploads = uploads)
        val originalException = RuntimeException("원래 예외")
        val rollbackException = RuntimeException("롤백 실패")

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } throws originalException
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) } throws rollbackException

        // When / Then
        // rollback 중 예외가 발생하면 rollback 예외가 전파됨 (원래 예외는 catch 블록 안에서 소실)
        val ex = shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        ex.message shouldBe "롤백 실패"
    }

    @Test
    @DisplayName("빈 uploads 목록 - 저장 0건 정상 처리")
    fun `빈 uploads 목록 - 저장 0건 정상 처리`() {
        // Given
        val command = makeCommand(uploads = emptyList())

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = emptyList())
        } returns emptyMap()

        every { poseRepository.saveAll(emptyList()) } returns emptyList()

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { poseRepository.saveAll(emptyList()) }
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }
}
