package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.GetUserInfoUseCase
import com.neki.user.client.MediaClient
import com.neki.user.client.NotificationClient
import com.neki.user.client.TermClient
import com.neki.user.dto.UserQuery
import com.neki.user.models.ProviderType
import com.neki.user.models.TermAgreementStatus
import com.neki.user.repository.UserRepository
import com.neki.user.service.UserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetUserInfoUseCaseTest {

    lateinit var userRepository: UserRepository
    lateinit var mediaClient: MediaClient
    lateinit var termClient: TermClient
    lateinit var notificationClient: NotificationClient
    lateinit var useCase: GetUserInfoUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        mediaClient = mockk()
        termClient = mockk()
        notificationClient = mockk()
        useCase = GetUserInfoUseCase(
            userService = UserService(userRepository, mockk()),
            mediaClient = mediaClient,
            termClient = termClient,
            notificationClient = notificationClient,
        )
    }

    private fun agreementStatus(requiredAgreed: Boolean, marketingAgreed: Boolean): TermAgreementStatus =
        TermAgreementStatus(requiredAgreed = requiredAgreed, marketingAgreed = marketingAgreed)

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
        every { termClient.getAgreementStatus(userId) } returns
            agreementStatus(requiredAgreed = true, marketingAgreed = false)
        every { notificationClient.isPushAgreed(userId) } returns true

        // When
        val result = useCase.execute(UserQuery.GetUser(userId = userId))

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
    @DisplayName("약관 동의 조회는 한 번만 호출된다")
    fun `약관 동의 조회는 한 번만 호출된다`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every { termClient.getAgreementStatus(userId) } returns
            agreementStatus(requiredAgreed = true, marketingAgreed = true)
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        useCase.execute(UserQuery.GetUser(userId = userId))

        // Then
        verify(exactly = 1) { termClient.getAgreementStatus(userId) }
    }

    @Test
    @DisplayName("프로필 이미지 없음 - storageKey null 반환")
    fun `프로필 이미지 없음 - storageKey null 반환`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, name = "테스트유저", profileImageId = null, providerType = ProviderType.KAKAO)

        every { userRepository.findById(userId) } returns user
        every { termClient.getAgreementStatus(userId) } returns
            agreementStatus(requiredAgreed = false, marketingAgreed = false)
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(UserQuery.GetUser(userId = userId))

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
            useCase.execute(UserQuery.GetUser(userId = 999L))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
        verify(exactly = 0) { mediaClient.getStorageKey(any(), any()) }
        verify(exactly = 0) { termClient.getAgreementStatus(any()) }
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
            useCase.execute(UserQuery.GetUser(userId = userId))
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
            termClient.getAgreementStatus(userId)
        } throws RuntimeException("약관 서비스 오류")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(UserQuery.GetUser(userId = userId))
        }
    }

    @Test
    @DisplayName("마케팅 동의 - marketingTerm true 반환")
    fun `마케팅 동의 - marketingTerm true 반환`() {
        // Given
        val userId = 1L
        val user = aUser(id = userId, profileImageId = null)

        every { userRepository.findById(userId) } returns user
        every { termClient.getAgreementStatus(userId) } returns
            agreementStatus(requiredAgreed = true, marketingAgreed = true)
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(UserQuery.GetUser(userId = userId))

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
        every { termClient.getAgreementStatus(userId) } returns
            agreementStatus(requiredAgreed = true, marketingAgreed = false)
        every { notificationClient.isPushAgreed(userId) } returns false

        // When
        val result = useCase.execute(UserQuery.GetUser(userId = userId))

        // Then
        result.marketingTerm shouldBe false
    }
}
