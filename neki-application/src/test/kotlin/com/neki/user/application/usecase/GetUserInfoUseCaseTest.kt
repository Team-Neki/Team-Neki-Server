package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.command.GetUserCommand
import com.neki.user.application.port.MediaClientPort
import com.neki.user.application.port.NotificationClientPort
import com.neki.user.application.port.TermClientPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.enums.ProviderType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetUserInfoUseCaseTest {

    lateinit var userRepository: UserRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var termClient: TermClientPort
    lateinit var notificationClient: NotificationClientPort
    lateinit var useCase: GetUserInfoUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        mediaClient = mockk()
        termClient = mockk()
        notificationClient = mockk()
        useCase = GetUserInfoUseCase(
            userRepository = userRepository,
            mediaClient = mediaClient,
            termClient = termClient,
            notificationClient = notificationClient,
        )
    }

    @Test
    @DisplayName("정상 조회 - 프로필 이미지 있음: user, storageKey, terms 동의 여부 반환")
    fun `정상 조회 - 프로필 이미지 있음 user, storageKey, terms 동의 여부 반환`() {
        // Given
        val userId = 1L
        val mediaId = 10L
        val user =
            aUser(
                id = userId,
                name = "테스트유저",
                email = "test@example.com",
                profileImageId = mediaId,
                providerType = ProviderType.KAKAO,
            )

        every { userRepository.findById(userId) } returns user
        every { mediaClient.getStorageKey(ownerId = userId, mediaId = mediaId) } returns "profile/image.jpg"
        every { termClient.hasAgreedToAllRequired(userId) } returns true
        every { termClient.hasAgreedToMarketing(userId) } returns false
        every { notificationClient.isPushAgreed(userId) } returns true

        // When
        val result = useCase.execute(GetUserCommand(userId = userId))

        // Then
        result.userId shouldBe userId
        result.name shouldBe "테스트유저"
        result.email shouldBe "test@example.com"
        result.objectKey shouldBe "profile/image.jpg"
        result.providerType shouldBe ProviderType.KAKAO
        result.agreeTerms shouldBe true
        result.marketingTerm shouldBe false
        result.pushAgreed shouldBe true
    }

    @Test
    @DisplayName("프로필 이미지 없음 - storageKey null 반환")
    fun `프로필 이미지 없음 - storageKey null 반환`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, name = "테스트유저", profileImageId = null, providerType = ProviderType.KAKAO)

        every { userRepository.findById(userId) } returns user
        every { termClient.hasAgreedToAllRequired(userId) } returns false
        every { termClient.hasAgreedToMarketing(userId) } returns false
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(GetUserCommand(userId = userId))

        // Then
        result.objectKey shouldBe null
        verify(exactly = 0) { mediaClient.getStorageKey(any(), any()) }
    }

    @Test
    @DisplayName("유저 미존재 시 NOT_FOUND_USER BusinessException 발생")
    fun `유저 미존재 시 NOT_FOUND_USER BusinessException 발생`() {
        // Given
        every { userRepository.findById(999L) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(GetUserCommand(userId = 999L))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
        verify(exactly = 0) { mediaClient.getStorageKey(any(), any()) }
        verify(exactly = 0) { termClient.hasAgreedToAllRequired(any()) }
        verify(exactly = 0) { termClient.hasAgreedToMarketing(any()) }
    }

    @Test
    @DisplayName("mediaClient 예외 - 프로필 이미지 조회 실패 시 전체 요청 실패")
    fun `mediaClient 예외 - 프로필 이미지 조회 실패 시 전체 요청 실패`() {
        // Given
        val userId = 1L
        val mediaId = 10L
        val user = aUser(id = userId, profileImageId = mediaId)

        every { userRepository.findById(userId) } returns user
        every {
            mediaClient.getStorageKey(ownerId = userId, mediaId = mediaId)
        } throws RuntimeException("미디어 조회 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(GetUserCommand(userId = userId))
        }
    }

    @Test
    @DisplayName("termClient 예외 - 약관 동의 조회 실패 시 전체 요청 실패")
    fun `termClient 예외 - 약관 동의 조회 실패 시 전체 요청 실패`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every {
            termClient.hasAgreedToAllRequired(userId)
        } throws RuntimeException("약관 서비스 오류")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(GetUserCommand(userId = userId))
        }
    }

    @Test
    @DisplayName("마케팅 동의 - marketingTerm true 반환")
    fun `마케팅 동의 - marketingTerm true 반환`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every { termClient.hasAgreedToAllRequired(userId) } returns true
        every { termClient.hasAgreedToMarketing(userId) } returns true
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(GetUserCommand(userId = userId))

        // Then
        result.marketingTerm shouldBe true
    }

    @Test
    @DisplayName("마케팅 미동의 - marketingTerm false 반환")
    fun `마케팅 미동의 - marketingTerm false 반환`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every { termClient.hasAgreedToAllRequired(userId) } returns true
        every { termClient.hasAgreedToMarketing(userId) } returns false
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(GetUserCommand(userId = userId))

        // Then
        result.marketingTerm shouldBe false
    }

    @Test
    @DisplayName("마케팅 termClient 예외 - 마케팅 동의 조회 실패 시 전체 요청 실패")
    fun `마케팅 termClient 예외 - 마케팅 동의 조회 실패 시 전체 요청 실패`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every { termClient.hasAgreedToAllRequired(userId) } returns true
        every {
            termClient.hasAgreedToMarketing(userId)
        } throws RuntimeException("마케팅 약관 서비스 오류")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(GetUserCommand(userId = userId))
        }
    }
}
