package com.neki.support.application.usecase

import com.neki.support.application.command.CheckLatestTermsAgreementCommand
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.testfixture.aTerm
import com.neki.testfixture.aUserTermAgreement
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CheckLatestTermsAgreementUseCaseTest {

    private lateinit var termRepository: TermRepositoryPort
    private lateinit var userTermAgreementRepository: UserTermAgreementRepositoryPort
    private lateinit var useCase: CheckLatestTermsAgreementUseCase

    @BeforeEach
    fun setUp() {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        useCase = CheckLatestTermsAgreementUseCase(termRepository, userTermAgreementRepository)
    }

    @Test
    @DisplayName("모든 활성 약관에 동의한 경우 - true를 반환한다")
    fun `모든 활성 약관에 동의한 경우 - true를 반환한다`() {
        // Given
        val userId = 1L
        val term1 = aTerm(id = 1L, version = "1.0.0")
        val term2 = aTerm(id = 2L, version = "1.0.0")
        val agreement1 = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")
        val agreement2 = aUserTermAgreement(userId = userId, termId = 2L, termVersion = "1.0.0")

        every { termRepository.findAllActiveRequiredTerms() } returns listOf(term1, term2)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement1, agreement2)

        // When
        val result = useCase.execute(CheckLatestTermsAgreementCommand(userId = userId))

        // Then
        result.hasAgreedToLatestTerms shouldBe true
    }

    @Test
    @DisplayName("일부 약관에 미동의한 경우 - false를 반환한다")
    fun `일부 약관에 미동의한 경우 - false를 반환한다`() {
        // Given
        val userId = 1L
        val term1 = aTerm(id = 1L, version = "1.0.0")
        val term2 = aTerm(id = 2L, version = "1.0.0")
        val agreement1 = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveRequiredTerms() } returns listOf(term1, term2)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement1)

        // When
        val result = useCase.execute(CheckLatestTermsAgreementCommand(userId = userId))

        // Then
        result.hasAgreedToLatestTerms shouldBe false
    }

    @Test
    @DisplayName("활성 약관이 없는 경우 - true를 반환한다")
    fun `활성 약관이 없는 경우 - true를 반환한다`() {
        // Given
        val userId = 1L

        every { termRepository.findAllActiveRequiredTerms() } returns emptyList()
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(CheckLatestTermsAgreementCommand(userId = userId))

        // Then
        result.hasAgreedToLatestTerms shouldBe true
    }

    @Test
    @DisplayName("약관 버전 불일치 - 동의한 약관의 version이 활성 약관과 다른 경우 false를 반환한다")
    fun `약관 버전 불일치 - 동의한 약관의 version이 활성 약관과 다른 경우 false를 반환한다`() {
        // Given
        val userId = 1L
        val term = aTerm(id = 1L, version = "2.0.0")
        val agreement = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveRequiredTerms() } returns listOf(term)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        val result = useCase.execute(CheckLatestTermsAgreementCommand(userId = userId))

        // Then
        result.hasAgreedToLatestTerms shouldBe false
    }

    @Test
    @DisplayName("유저 동의 목록이 비어있고 활성 약관이 있는 경우 - false를 반환한다")
    fun `유저 동의 목록이 비어있고 활성 약관이 있는 경우 - false를 반환한다`() {
        // Given
        val userId = 1L
        val term = aTerm(id = 1L, version = "1.0.0")

        every { termRepository.findAllActiveRequiredTerms() } returns listOf(term)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(CheckLatestTermsAgreementCommand(userId = userId))

        // Then
        result.hasAgreedToLatestTerms shouldBe false
    }
}
