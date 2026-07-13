package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aUser
import com.neki.user.application.command.UpdateUserProfileImageCommand
import com.neki.user.application.contract.MediaAvailability
import com.neki.user.application.port.MediaClientPort
import com.neki.user.application.port.UserRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateUserProfileImageUseCaseTest {

    lateinit var userRepository: UserRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var transactionRunner: FakeTransactionRunner
    lateinit var useCase: UpdateUserProfileImageUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        mediaClient = mockk()
        transactionRunner = FakeTransactionRunner()
        useCase = UpdateUserProfileImageUseCase(
            userRepository = userRepository,
            mediaClient = mediaClient,
            transactionRunner = transactionRunner,
        )
    }

    @Test
    @DisplayName("새 이미지 설정 - 미디어 확인 후 프로필 업데이트 및 이전 이미지 삭제")
    fun `새 이미지 설정 - 미디어 확인 후 프로필 업데이트 및 이전 이미지 삭제`() {
        // Given
        val userId = 1L
        val oldMediaId = 10L
        val newMediaId = 20L
        val user = aUser(id = userId, profileImageId = oldMediaId)

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } returns user
        every { mediaClient.deleteMedia(ownerId = userId, mediaIds = oldMediaId) } just runs

        // When
        useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))

        // Then
        user.profileImageId shouldBe newMediaId
        verify(exactly = 1) { mediaClient.deleteMedia(ownerId = userId, mediaIds = oldMediaId) }
    }

    @Test
    @DisplayName("동일 이미지 설정 시 멱등성 - no-op")
    fun `동일 이미지 설정 시 멱등성 - no-op`() {
        // Given
        val userId = 1L
        val sameMediaId = 10L
        val user = aUser(id = userId, profileImageId = sameMediaId)

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = sameMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } returns user

        // When
        useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = sameMediaId))

        // Then
        user.profileImageId shouldBe sameMediaId
        verify(exactly = 0) { mediaClient.deleteMedia(any(), any()) }
    }

    @Test
    @DisplayName("미디어 사용 불가 시 NOT_FOUND BusinessException 발생")
    fun `미디어 사용 불가 시 NOT_FOUND BusinessException 발생`() {
        // Given
        val userId = 1L
        val newMediaId = 20L

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.UNAVAILABLE

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    @DisplayName("트랜잭션 실패 시 미디어 롤백 호출 확인")
    fun `트랜잭션 실패 시 미디어 롤백 호출 확인`() {
        // Given
        val userId = 1L
        val newMediaId = 20L

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } throws RuntimeException("DB 오류")
        every { mediaClient.rollbackMediasUploaded(ownerId = userId, mediaIds = listOf(newMediaId)) } just runs

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))
        }
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(ownerId = userId, mediaIds = listOf(newMediaId)) }
    }

    @Test
    @DisplayName("기본 이미지로 변경 (null) - 프로필 null 설정 및 이전 이미지 삭제")
    fun `기본 이미지로 변경 (null) - 프로필 null 설정 및 이전 이미지 삭제`() {
        // Given
        val userId = 1L
        val oldMediaId = 10L
        val user = aUser(id = userId, profileImageId = oldMediaId)

        every { userRepository.findById(userId) } returns user
        every { mediaClient.deleteMedia(ownerId = userId, mediaIds = oldMediaId) } just runs

        // When
        useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = null))

        // Then
        user.profileImageId shouldBe null
        verify(exactly = 1) { mediaClient.deleteMedia(ownerId = userId, mediaIds = oldMediaId) }
    }

    @Test
    @DisplayName("이미 기본 이미지인 경우 멱등성 - no-op")
    fun `이미 기본 이미지인 경우 멱등성 - no-op`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user

        // When
        useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = null))

        // Then
        user.profileImageId shouldBe null
        verify(exactly = 0) { mediaClient.deleteMedia(any(), any()) }
    }

    @Test
    @DisplayName("롤백 중 예외 발생 - 롤백 예외가 전파됨 (원래 예외가 아닌 롤백 예외)")
    fun `롤백 중 예외 발생 - 롤백 예외가 전파됨 (원래 예외가 아닌 롤백 예외)`() {
        // Given
        val userId = 1L
        val newMediaId = 20L
        val originalException = RuntimeException("원래 오류")
        val rollbackException = RuntimeException("롤백 오류")

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } throws originalException
        every {
            mediaClient.rollbackMediasUploaded(ownerId = userId, mediaIds = listOf(newMediaId))
        } throws rollbackException

        // When & Then
        // catch 블록에서 rollbackMediasUploaded가 예외를 던지면, rollback 예외가 전파됨 (원래 예외는 마스킹됨)
        val thrownException = shouldThrow<RuntimeException> {
            useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))
        }
        thrownException.message shouldBe "롤백 오류"
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(ownerId = userId, mediaIds = listOf(newMediaId)) }
    }

    @Test
    @DisplayName("이전 이미지 삭제 실패 - 트랜잭션 성공 후 deleteMedia 예외 발생 시 프로필은 업데이트됨")
    fun `이전 이미지 삭제 실패 - 트랜잭션 성공 후 deleteMedia 예외 발생 시 프로필은 업데이트됨`() {
        // Given
        val userId = 1L
        val oldMediaId = 10L
        val newMediaId = 20L
        val user = aUser(id = userId, profileImageId = oldMediaId)

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } returns user
        every { mediaClient.deleteMedia(ownerId = userId, mediaIds = oldMediaId) } throws RuntimeException("삭제 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))
        }
        // 프로필은 트랜잭션 내에서 이미 업데이트됨
        user.profileImageId shouldBe newMediaId
    }

    @Test
    @DisplayName("이전 프로필 이미지 없음 (oldMediaId=null) - 새 이미지 설정 시 이전 이미지 삭제 skip")
    fun `이전 프로필 이미지 없음 (oldMediaId=null) - 새 이미지 설정 시 이전 이미지 삭제 skip`() {
        // Given
        val userId = 1L
        val newMediaId = 20L
        val user = aUser(id = userId, profileImageId = null) // 기존 프로필 이미지 없음

        every { mediaClient.verifyMediaUploaded(ownerId = userId, mediaId = newMediaId) } returns
            MediaAvailability.AVAILABLE
        every { userRepository.findById(userId) } returns user

        // When
        useCase.execute(UpdateUserProfileImageCommand(userId = userId, mediaId = newMediaId))

        // Then
        user.profileImageId shouldBe newMediaId
        verify(exactly = 0) { mediaClient.deleteMedia(any(), any()) }
    }
}
