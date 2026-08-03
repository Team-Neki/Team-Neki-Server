package com.neki.support.application.usecase

import com.neki.support.TermRepository
import com.neki.support.UserTermAgreementHistRepository
import com.neki.support.UserTermAgreementRepository
import com.neki.support.application.RevokeOptionalTermsUseCase
import com.neki.support.dto.TermCommand
import com.neki.support.models.TermAgreementAction
import com.neki.support.models.UserTermAgreementHist
import com.neki.support.service.TermService
import com.neki.testfixture.aTerm
import com.neki.testfixture.aUserTermAgreement
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RevokeOptionalTermsUseCaseTest {

    private lateinit var termRepository: TermRepository
    private lateinit var userTermAgreementRepository: UserTermAgreementRepository
    private lateinit var userTermAgreementHistRepository: UserTermAgreementHistRepository
    private lateinit var useCase: RevokeOptionalTermsUseCase

    @BeforeEach
    fun setUp() {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        userTermAgreementHistRepository = mockk()
        useCase = RevokeOptionalTermsUseCase(
            TermService(termRepository, userTermAgreementRepository, userTermAgreementHistRepository),
        )
    }

    @Test
    @DisplayName("동의한 선택 약관이 있는 경우 - 해당 약관을 철회하고 이력을 저장한다")
    fun `동의한 선택 약관이 있는 경우 - 해당 약관을 철회하고 이력을 저장한다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)
        val optionalTerm = aTerm(id = 2L, isRequired = false)

        // 유저는 필수(1) + 선택(2) 약관에 모두 동의한 상태
        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
            aUserTermAgreement(userId = userId, termId = 2L),
        )

        val deletedTermIds = slot<List<Long>>()
        val savedHists = slot<List<UserTermAgreementHist>>()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(userId, capture(deletedTermIds)) } returns Unit
        every { userTermAgreementHistRepository.saveAll(capture(savedHists)) } returns Unit

        // When
        useCase.execute(TermCommand.RevokeOptionalTerms(userId))

        // Then - 선택 약관(2)만 철회 대상
        deletedTermIds.captured shouldContainExactlyInAnyOrder listOf(2L)

        savedHists.captured.size shouldBe 1
        val hist = savedHists.captured.first()
        hist.userId shouldBe userId
        hist.termId shouldBe 2L
        hist.action shouldBe TermAgreementAction.WITHDRAWN

        verify(exactly = 1) { userTermAgreementRepository.deleteAllByUserIdAndTermIds(userId, any()) }
        verify(exactly = 1) { userTermAgreementHistRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("동의한 선택 약관이 없는 경우 - 삭제/이력 저장을 수행하지 않는다")
    fun `동의한 선택 약관이 없는 경우 - 삭제 이력 저장을 수행하지 않는다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)
        val optionalTerm = aTerm(id = 2L, isRequired = false)

        // 유저는 필수 약관(1)에만 동의 - 선택 약관(2)에는 미동의
        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
        )

        // When
        useCase.execute(TermCommand.RevokeOptionalTerms(userId))

        // Then
        verify(exactly = 0) { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) }
        verify(exactly = 0) { userTermAgreementHistRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("활성 선택 약관 자체가 없는 경우 - 삭제/이력 저장을 수행하지 않는다")
    fun `활성 선택 약관 자체가 없는 경우 - 삭제 이력 저장을 수행하지 않는다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
        )

        // When
        useCase.execute(TermCommand.RevokeOptionalTerms(userId))

        // Then
        verify(exactly = 0) { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) }
        verify(exactly = 0) { userTermAgreementHistRepository.saveAll(any()) }
    }
}
