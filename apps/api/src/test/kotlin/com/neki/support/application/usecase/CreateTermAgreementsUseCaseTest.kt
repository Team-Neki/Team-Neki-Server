package com.neki.support.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.CreateTermAgreementsUseCase
import com.neki.support.dto.TermCommand
import com.neki.support.models.TermAgreementAction
import com.neki.support.models.UserTermAgreementHist
import com.neki.support.repository.TermRepository
import com.neki.support.repository.UserTermAgreementHistRepository
import com.neki.support.repository.UserTermAgreementRepository
import com.neki.support.service.TermService
import com.neki.testfixture.aTerm
import com.neki.testfixture.aUserTermAgreement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CreateTermAgreementsUseCaseTest {

    private lateinit var termRepository: TermRepository
    private lateinit var userTermAgreementRepository: UserTermAgreementRepository
    private lateinit var userTermAgreementHistRepository: UserTermAgreementHistRepository
    private lateinit var useCase: CreateTermAgreementsUseCase

    @BeforeEach
    fun setUp() {
        termRepository = mockk()
        userTermAgreementRepository = mockk()
        userTermAgreementHistRepository = mockk()
        useCase =
            CreateTermAgreementsUseCase(
                TermService(termRepository, userTermAgreementRepository, userTermAgreementHistRepository),
            )
    }

    @Test
    @DisplayName("정상 동의 - 필수 약관 모두 포함 시 저장에 성공한다")
    fun `정상 동의 - 필수 약관 모두 포함 시 저장에 성공한다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(any()) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 1L, agreed = true),
                TermCommand.TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 2) { userTermAgreementRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("필수 약관 누락 - BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다")
    fun `필수 약관 누락 - BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)
        val optionalTerm = aTerm(id = 2L, isRequired = false)

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        exception.resultCode shouldBe ResultCode.REQUIRED_TERMS_NOT_AGREED
        verify(exactly = 0) { userTermAgreementRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("선택 약관만 미동의 - 필수 약관이 모두 포함된 경우 정상 처리된다")
    fun `선택 약관만 미동의 - 필수 약관이 모두 포함된 경우 정상 처리된다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(any()) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 1L, agreed = true),
                TermCommand.TermAgreementItem(termId = 2L, agreed = false),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 2) { userTermAgreementRepository.saveAll(any()) }
        verify(exactly = 1) { userTermAgreementRepository.deleteAllByUserIdAndTermIds(userId, listOf(2L)) }
    }

    @Test
    @DisplayName("빈 agreements 목록 - 필수 약관이 존재할 때 BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다")
    fun `빈 agreements 목록 - 필수 약관이 존재할 때 BusinessException(REQUIRED_TERMS_NOT_AGREED)을 던진다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true)

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns emptyList()

        val command = TermCommand.CreateTermAgreements(
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

    @Test
    @DisplayName("비존재 termId 포함 - 활성 약관에 없는 ID가 포함되면 NOT_FOUND를 던진다")
    fun `비존재 termId 포함 - 활성 약관에 없는 ID가 포함되면 NOT_FOUND를 던진다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm)

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 1L, agreed = true),
                TermCommand.TermAgreementItem(termId = 999L, agreed = true),
            ),
        )

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { userTermAgreementRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("선택 약관 최초 동의 시 AGREED 이력이 저장된다")
    fun `선택 약관 최초 동의 시 AGREED 이력이 저장된다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")
        val histSlot = slot<List<UserTermAgreementHist>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns
            listOf(aUserTermAgreement(userId = userId, termId = 1L))
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(capture(histSlot)) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        histSlot.captured shouldHaveSize 1
        histSlot.captured[0].action shouldBe TermAgreementAction.AGREED
        histSlot.captured[0].termId shouldBe 2L
    }

    @Test
    @DisplayName("선택 약관 철회 시 WITHDRAWN 이력이 저장된다")
    fun `선택 약관 철회 시 WITHDRAWN 이력이 저장된다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")
        val histSlot = slot<List<UserTermAgreementHist>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
            aUserTermAgreement(userId = userId, termId = 2L),
        )
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(capture(histSlot)) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 2L, agreed = false),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        histSlot.captured shouldHaveSize 1
        histSlot.captured[0].action shouldBe TermAgreementAction.WITHDRAWN
        histSlot.captured[0].termId shouldBe 2L
    }

    @Test
    @DisplayName("이미 동의한 선택 약관을 다시 동의해도 이력이 쌓이지 않는다")
    fun `이미 동의한 선택 약관을 다시 동의해도 이력이 쌓이지 않는다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")
        val histSlot = slot<List<UserTermAgreementHist>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
            aUserTermAgreement(userId = userId, termId = 2L),
        )
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(capture(histSlot)) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 2L, agreed = true),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        histSlot.captured.shouldBeEmpty()
        verify(exactly = 1) { userTermAgreementRepository.saveAll(emptyList()) }
    }

    @Test
    @DisplayName("이미 미동의한 선택 약관을 다시 미동의해도 이력이 쌓이지 않는다")
    fun `이미 미동의한 선택 약관을 다시 미동의해도 이력이 쌓이지 않는다`() {
        // Given
        val userId = 1L
        val requiredTerm = aTerm(id = 1L, isRequired = true, version = "1.0.0")
        val optionalTerm = aTerm(id = 2L, isRequired = false, version = "1.0.0")
        val histSlot = slot<List<UserTermAgreementHist>>()

        every { termRepository.findAllActiveTerms() } returns listOf(requiredTerm, optionalTerm)
        every { userTermAgreementRepository.findByUserId(userId) } returns listOf(
            aUserTermAgreement(userId = userId, termId = 1L),
        )
        every { userTermAgreementRepository.saveAll(any()) } returns emptyList()
        every { userTermAgreementRepository.deleteAllByUserIdAndTermIds(any(), any()) } just runs
        every { userTermAgreementHistRepository.saveAll(capture(histSlot)) } just runs

        val command = TermCommand.CreateTermAgreements(
            userId = userId,
            agreements = listOf(
                TermCommand.TermAgreementItem(termId = 2L, agreed = false),
            ),
        )

        // When
        useCase.execute(command)

        // Then
        histSlot.captured.shouldBeEmpty()
        verify(exactly = 1) { userTermAgreementRepository.deleteAllByUserIdAndTermIds(userId, listOf(2L)) }
    }
}
