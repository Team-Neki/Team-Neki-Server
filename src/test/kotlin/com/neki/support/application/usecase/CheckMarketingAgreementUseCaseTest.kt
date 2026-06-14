package com.neki.support.application.usecase

import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.enums.TermType
import com.neki.testfixture.aTerm
import com.neki.testfixture.aUserTermAgreement
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CheckMarketingAgreementUseCaseTest {

    private lateinit var termRepository: TermRepositoryPort
    private lateinit var userTermAgreementRepository: UserTermAgreementRepositoryPort
    private lateinit var useCase: CheckMarketingAgreementUseCase

    @BeforeEach
    fun setUp() {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        useCase = CheckMarketingAgreementUseCase(termRepository, userTermAgreementRepository)
    }

    @Test
    @DisplayName("활성 마케팅 약관이 없는 경우 - false를 반환한다")
    fun `활성 마케팅 약관이 없는 경우 - false를 반환한다`() {
        // Given
        val userId = 1L
        every { termRepository.findActiveByTermType(TermType.MARKETING) } returns null

        // When
        val result = useCase.execute(userId)

        // Then
        result.agreed shouldBe false
    }

    @Test
    @DisplayName("마케팅 약관에 동의한 경우 - true를 반환한다")
    fun `마케팅 약관에 동의한 경우 - true를 반환한다`() {
        // Given
        val userId = 1L
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false)
        val agreement = aUserTermAgreement(userId = userId, termId = 5L)

        every { termRepository.findActiveByTermType(TermType.MARKETING) } returns marketingTerm
        every { userTermAgreementRepository.findByUserIdAndTermId(userId, 5L) } returns agreement

        // When
        val result = useCase.execute(userId)

        // Then
        result.agreed shouldBe true
    }

    @Test
    @DisplayName("마케팅 약관에 동의 기록이 없는 경우 - false를 반환한다")
    fun `마케팅 약관에 동의 기록이 없는 경우 - false를 반환한다`() {
        // Given
        val userId = 1L
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false)

        every { termRepository.findActiveByTermType(TermType.MARKETING) } returns marketingTerm
        every { userTermAgreementRepository.findByUserIdAndTermId(userId, 5L) } returns null

        // When
        val result = useCase.execute(userId)

        // Then
        result.agreed shouldBe false
    }
}
