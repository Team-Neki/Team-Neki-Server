package com.neki.support.application.usecase

import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.domain.enums.TermType
import com.neki.testfixture.aTerm
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetTermsUseCaseTest : FunSpec({

    lateinit var termRepository: TermRepositoryPort
    lateinit var useCase: GetTermsUseCase

    beforeTest {
        termRepository = mockk()
        useCase = GetTermsUseCase(termRepository)
    }

    test("약관 목록 반환 - port가 Term 리스트 반환 시 TermInfo로 올바르게 매핑된다") {
        // Given
        val term1 = aTerm(id = 1L, termType = TermType.SERVICE, title = "서비스 이용약관", url = "https://example.com/service", isRequired = true)
        val term2 = aTerm(id = 2L, termType = TermType.PRIVACY, title = "개인정보 처리방침", url = "https://example.com/privacy", isRequired = true)
        every { termRepository.findAllActiveTerms() } returns listOf(term1, term2)

        // When
        val result = useCase.execute()

        // Then
        result.terms.size shouldBe 2
        with(result.terms[0]) {
            id shouldBe 1L
            termType shouldBe TermType.SERVICE
            title shouldBe "서비스 이용약관"
            url shouldBe "https://example.com/service"
            isRequired shouldBe true
        }
        with(result.terms[1]) {
            id shouldBe 2L
            termType shouldBe TermType.PRIVACY
            title shouldBe "개인정보 처리방침"
            url shouldBe "https://example.com/privacy"
            isRequired shouldBe true
        }
    }

    test("빈 목록 - port가 빈 리스트 반환 시 빈 결과를 반환한다") {
        // Given
        every { termRepository.findAllActiveTerms() } returns emptyList()

        // When
        val result = useCase.execute()

        // Then
        result.terms.shouldBeEmpty()
    }
})
