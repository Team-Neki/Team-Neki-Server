package com.neki.support.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.CreateTermAgreementsCommand
import com.neki.support.application.command.TermAgreementItem
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.UserTermAgreement
import com.neki.testfixture.aTerm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CreateTermAgreementsUseCaseTest : FunSpec({

    lateinit var termRepository: TermRepositoryPort
    lateinit var userTermAgreementRepository: UserTermAgreementRepositoryPort
    lateinit var useCase: CreateTermAgreementsUseCase

    beforeTest {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        useCase = CreateTermAgreementsUseCase(termRepository, userTermAgreementRepository)
    }

    test("정상 동의 - 필수 약관 모두 포함 시 저장에 성공한다") {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")
        val savedSlot = slot<List<UserTermAgreement>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { termRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.saveAll(capture(savedSlot)) } returns emptyList()

        val command = CreateTermAgreementsCommand(
            userId = userId,
            agreements = listOf(
                TermAgreementItem(termId = 1L, agreed = true),
                TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { userTermAgreementRepository.saveAll(any()) }
        savedSlot.captured.size shouldBe 2
    }

    test("필수 약관 누락 - BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다") {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)
        val optionalTerm = aTerm(id = 2L, isRequired = false)

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)

        val command = CreateTermAgreementsCommand(
            userId = userId,
            agreements = listOf(
                TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        exception.resultCode shouldBe ResultCode.REQUIRED_TERMS_NOT_AGREED
        verify(exactly = 0) { userTermAgreementRepository.saveAll(any()) }
    }

    test("선택 약관만 미동의 - 필수 약관이 모두 포함된 경우 정상 처리된다") {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { termRepository.findAllByIds(listOf(1L)) } returns listOf(requiredTerm)
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()

        val command = CreateTermAgreementsCommand(
            userId = userId,
            agreements = listOf(
                TermAgreementItem(termId = 1L, agreed = true),
                TermAgreementItem(termId = 2L, agreed = false),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { userTermAgreementRepository.saveAll(any()) }
    }

    test("빈 agreements 목록 - 필수 약관이 존재할 때 BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다") {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm)

        val command = CreateTermAgreementsCommand(
            userId = userId,
            agreements = emptyList(),
        )

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        exception.resultCode shouldBe ResultCode.REQUIRED_TERMS_NOT_AGREED
        verify(exactly = 0) { userTermAgreementRepository.saveAll(any()) }
    }

    test("존재하지 않는 termId 포함 - agreedTermIds에 활성 약관에 없는 ID는 findAllByIds에서 제외되어 무시된다") {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val savedSlot = slot<List<UserTermAgreement>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm)
        every { termRepository.findAllByIds(listOf(1L, 999L)) } returns listOf(requiredTerm)
        every { userTermAgreementRepository.saveAll(capture(savedSlot)) } returns emptyList()

        val command = CreateTermAgreementsCommand(
            userId = userId,
            agreements = listOf(
                TermAgreementItem(termId = 1L, agreed = true),
                TermAgreementItem(termId = 999L, agreed = true),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { userTermAgreementRepository.saveAll(any()) }
        savedSlot.captured.size shouldBe 1
        savedSlot.captured[0].id.termId shouldBe 1L
    }
})
