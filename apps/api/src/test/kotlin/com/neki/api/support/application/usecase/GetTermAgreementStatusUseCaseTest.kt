package com.neki.api.support.application.usecase

import com.neki.api.support.application.GetTermAgreementStatusUseCase
import com.neki.api.testfixture.aTerm
import com.neki.api.testfixture.aUserTermAgreement
import com.neki.domain.support.dto.TermQuery
import com.neki.domain.support.models.TermType
import com.neki.domain.support.repository.TermRepository
import com.neki.domain.support.repository.UserTermAgreementRepository
import com.neki.domain.support.service.TermService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetTermAgreementStatusUseCaseTest {

    private lateinit var termRepository: TermRepository
    private lateinit var userTermAgreementRepository: UserTermAgreementRepository
    private lateinit var useCase: GetTermAgreementStatusUseCase

    private val userId = 1L

    @BeforeEach
    fun setUp() {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        useCase = GetTermAgreementStatusUseCase(TermService(termRepository, userTermAgreementRepository, mockk()))
    }

    @Test
    @DisplayName("모든 필수 약관에 동의한 경우 - requiredAgreed true를 반환한다")
    fun `모든 필수 약관에 동의한 경우 - requiredAgreed true를 반환한다`() {
        // Given
        val term1 = aTerm(id = 1L, version = "1.0.0")
        val term2 = aTerm(id = 2L, version = "1.0.0")
        val agreement1 = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")
        val agreement2 = aUserTermAgreement(userId = userId, termId = 2L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(term1, term2)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement1, agreement2)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe true
    }

    @Test
    @DisplayName("일부 필수 약관에 미동의한 경우 - requiredAgreed false를 반환한다")
    fun `일부 필수 약관에 미동의한 경우 - requiredAgreed false를 반환한다`() {
        // Given
        val term1 = aTerm(id = 1L, version = "1.0.0")
        val term2 = aTerm(id = 2L, version = "1.0.0")
        val agreement1 = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(term1, term2)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement1)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe false
    }

    @Test
    @DisplayName("활성 약관이 없는 경우 - requiredAgreed true를 반환한다")
    fun `활성 약관이 없는 경우 - requiredAgreed true를 반환한다`() {
        // Given
        every { termRepository.findAllActiveTerms() } returns emptyList()
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe true
        result.marketingAgreed shouldBe false
    }

    @Test
    @DisplayName("필수 약관 버전 불일치 - 동의한 version이 활성 version과 다르면 requiredAgreed false를 반환한다")
    fun `필수 약관 버전 불일치 - 동의한 version이 활성 version과 다르면 requiredAgreed false를 반환한다`() {
        // Given
        val term = aTerm(id = 1L, version = "2.0.0")
        val agreement = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(term)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe false
    }

    @Test
    @DisplayName("유저 동의 목록이 비어있고 필수 약관이 있는 경우 - requiredAgreed false를 반환한다")
    fun `유저 동의 목록이 비어있고 필수 약관이 있는 경우 - requiredAgreed false를 반환한다`() {
        // Given
        val term = aTerm(id = 1L, version = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(term)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe false
    }

    @Test
    @DisplayName("선택 약관 미동의는 requiredAgreed에 영향을 주지 않는다")
    fun `선택 약관 미동의는 requiredAgreed에 영향을 주지 않는다`() {
        // Given
        val requiredTerm = aTerm(id = 1L, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, termType = TermType.MARKETING, isRequired = false)
        val agreement = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.requiredAgreed shouldBe true
        result.marketingAgreed shouldBe false
    }

    @Test
    @DisplayName("활성 마케팅 약관이 없는 경우 - marketingAgreed false를 반환한다")
    fun `활성 마케팅 약관이 없는 경우 - marketingAgreed false를 반환한다`() {
        // Given
        every { termRepository.findAllActiveTerms() } returns listOf(aTerm(id = 1L))
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.marketingAgreed shouldBe false
    }

    @Test
    @DisplayName("마케팅 약관에 동의한 경우 - marketingAgreed true를 반환한다")
    fun `마케팅 약관에 동의한 경우 - marketingAgreed true를 반환한다`() {
        // Given
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false)
        val agreement = aUserTermAgreement(userId = userId, termId = 5L)

        every { termRepository.findAllActiveTerms() } returns listOf(marketingTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.marketingAgreed shouldBe true
    }

    @Test
    @DisplayName("마케팅 약관에 동의 기록이 없는 경우 - marketingAgreed false를 반환한다")
    fun `마케팅 약관에 동의 기록이 없는 경우 - marketingAgreed false를 반환한다`() {
        // Given
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false)

        every { termRepository.findAllActiveTerms() } returns listOf(marketingTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.marketingAgreed shouldBe false
    }

    @Test
    @DisplayName("마케팅 약관은 버전이 달라도 동의 기록만 있으면 marketingAgreed true를 반환한다")
    fun `마케팅 약관은 버전이 달라도 동의 기록만 있으면 marketingAgreed true를 반환한다`() {
        // Given: 필수 약관과 달리 마케팅은 버전 일치를 요구하지 않는다
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false, version = "2.0.0")
        val agreement = aUserTermAgreement(userId = userId, termId = 5L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(marketingTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        val result = useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        result.marketingAgreed shouldBe true
    }

    @Test
    @DisplayName("필수 약관과 마케팅 약관을 조회해도 리포지토리는 각각 한 번만 호출된다")
    fun `필수 약관과 마케팅 약관을 조회해도 리포지토리는 각각 한 번만 호출된다`() {
        // Given
        val requiredTerm = aTerm(id = 1L, version = "1.0.0")
        val marketingTerm = aTerm(id = 5L, termType = TermType.MARKETING, isRequired = false)
        val agreement = aUserTermAgreement(userId = userId, termId = 1L, termVersion = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, marketingTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(agreement)

        // When
        useCase.execute(TermQuery.GetAgreementStatus(userId = userId))

        // Then
        verify(exactly = 1) { termRepository.findAllActiveTerms() }
        verify(exactly = 1) { userTermAgreementRepository.findByUserId(userId) }
        verify(exactly = 0) { termRepository.findActiveByTermType(any()) }
        verify(exactly = 0) { userTermAgreementRepository.findByUserIdAndTermId(any(), any()) }
    }
}
